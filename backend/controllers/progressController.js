const { Progress, Workout } = require('../models');
const { Op } = require('sequelize');

// Get progress history
const getProgress = async (req, res) => {
    try {
        const { limit = 30 } = req.query;

        const progress = await Progress.findAll({
            where: { userId: req.userId },
            order: [['recordDate', 'DESC']],
            limit: parseInt(limit)
        });

        res.json({
            success: true,
            data: progress.reverse() // Oldest first for charts
        });
    } catch (error) {
        console.error('Get progress error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch progress.'
        });
    }
};

// Record new progress entry
const recordProgress = async (req, res) => {
    try {
        const { weight, recordDate, notes } = req.body;
        const date = recordDate || new Date().toISOString().split('T')[0];

        // Calculate workout stats for this date
        const startOfWeek = new Date(date);
        startOfWeek.setDate(startOfWeek.getDate() - startOfWeek.getDay());

        const workouts = await Workout.findAll({
            where: {
                userId: req.userId,
                workoutDate: {
                    [Op.gte]: startOfWeek.toISOString().split('T')[0],
                    [Op.lte]: date
                }
            }
        });

        const totalWorkouts = workouts.length;
        const totalCalories = workouts.reduce((sum, w) => sum + (w.caloriesBurned || 0), 0);
        const totalDuration = workouts.reduce((sum, w) => sum + w.durationMinutes, 0);

        // Check if entry exists for this date
        let progress = await Progress.findOne({
            where: { userId: req.userId, recordDate: date }
        });

        if (progress) {
            await progress.update({
                weight: weight !== undefined ? weight : progress.weight,
                totalWorkouts,
                totalCalories,
                totalDuration,
                notes: notes !== undefined ? notes : progress.notes
            });
        } else {
            progress = await Progress.create({
                userId: req.userId,
                weight,
                recordDate: date,
                totalWorkouts,
                totalCalories,
                totalDuration,
                notes
            });
        }

        res.status(201).json({
            success: true,
            message: 'Progress recorded successfully.',
            data: progress
        });
    } catch (error) {
        console.error('Record progress error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to record progress.'
        });
    }
};

// Get aggregated statistics
const getStats = async (req, res) => {
    try {
        const { period = 'week' } = req.query;

        let startDate = new Date();
        switch (period) {
            case 'week':
                startDate.setDate(startDate.getDate() - 7);
                break;
            case 'month':
                startDate.setMonth(startDate.getMonth() - 1);
                break;
            case 'year':
                startDate.setFullYear(startDate.getFullYear() - 1);
                break;
        }

        const workouts = await Workout.findAll({
            where: {
                userId: req.userId,
                workoutDate: { [Op.gte]: startDate.toISOString().split('T')[0] }
            }
        });

        const progressRecords = await Progress.findAll({
            where: {
                userId: req.userId,
                recordDate: { [Op.gte]: startDate.toISOString().split('T')[0] }
            },
            order: [['recordDate', 'ASC']]
        });

        const stats = {
            period,
            workouts: {
                total: workouts.length,
                totalDuration: workouts.reduce((sum, w) => sum + w.durationMinutes, 0),
                totalCalories: workouts.reduce((sum, w) => sum + (w.caloriesBurned || 0), 0),
                avgDuration: workouts.length ? Math.round(workouts.reduce((sum, w) => sum + w.durationMinutes, 0) / workouts.length) : 0,
                byType: {}
            },
            weight: {
                current: progressRecords.length ? progressRecords[progressRecords.length - 1].weight : null,
                start: progressRecords.length ? progressRecords[0].weight : null,
                change: null,
                history: progressRecords.map(p => ({ date: p.recordDate, weight: p.weight }))
            },
            calories: {
                history: []
            }
        };

        // Calculate weight change
        if (stats.weight.current && stats.weight.start) {
            stats.weight.change = Math.round((stats.weight.current - stats.weight.start) * 10) / 10;
        }

        // Workouts by type
        workouts.forEach(w => {
            if (!stats.workouts.byType[w.type]) {
                stats.workouts.byType[w.type] = { count: 0, duration: 0, calories: 0 };
            }
            stats.workouts.byType[w.type].count++;
            stats.workouts.byType[w.type].duration += w.durationMinutes;
            stats.workouts.byType[w.type].calories += w.caloriesBurned || 0;
        });

        // Daily calories
        const caloriesByDate = {};
        workouts.forEach(w => {
            if (!caloriesByDate[w.workoutDate]) {
                caloriesByDate[w.workoutDate] = 0;
            }
            caloriesByDate[w.workoutDate] += w.caloriesBurned || 0;
        });
        stats.calories.history = Object.entries(caloriesByDate)
            .map(([date, calories]) => ({ date, calories }))
            .sort((a, b) => new Date(a.date) - new Date(b.date));

        res.json({
            success: true,
            data: stats
        });
    } catch (error) {
        console.error('Get stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch statistics.'
        });
    }
};

// Get dashboard data
const getDashboard = async (req, res) => {
    try {
        const today = new Date();
        const startOfWeek = new Date(today);
        startOfWeek.setDate(today.getDate() - today.getDay());
        const startOfWeekStr = startOfWeek.toISOString().split('T')[0];

        // This week's workouts
        const weekWorkouts = await Workout.findAll({
            where: {
                userId: req.userId,
                workoutDate: { [Op.gte]: startOfWeekStr }
            }
        });

        // Latest progress
        const latestProgress = await Progress.findOne({
            where: { userId: req.userId },
            order: [['recordDate', 'DESC']]
        });

        // Recent workouts
        const recentWorkouts = await Workout.findAll({
            where: { userId: req.userId },
            order: [['workoutDate', 'DESC'], ['createdAt', 'DESC']],
            limit: 5
        });

        const dashboard = {
            weekSummary: {
                workouts: weekWorkouts.length,
                duration: weekWorkouts.reduce((sum, w) => sum + w.durationMinutes, 0),
                calories: weekWorkouts.reduce((sum, w) => sum + (w.caloriesBurned || 0), 0)
            },
            currentWeight: latestProgress?.weight || null,
            recentWorkouts,
            recommendations: generateRecommendations(weekWorkouts, req.user)
        };

        res.json({
            success: true,
            data: dashboard
        });
    } catch (error) {
        console.error('Get dashboard error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch dashboard.'
        });
    }
};

// Generate simple recommendations
function generateRecommendations(weekWorkouts, user) {
    const recommendations = [];

    if (weekWorkouts.length === 0) {
        recommendations.push({
            type: 'motivation',
            message: 'Start your week strong! Log your first workout today.',
            icon: '💪'
        });
    } else if (weekWorkouts.length < 3) {
        recommendations.push({
            type: 'frequency',
            message: `You've done ${weekWorkouts.length} workout(s) this week. Aim for at least 3!`,
            icon: '🎯'
        });
    } else {
        recommendations.push({
            type: 'praise',
            message: `Great job! ${weekWorkouts.length} workouts this week. Keep it up!`,
            icon: '🌟'
        });
    }

    const workoutTypes = new Set(weekWorkouts.map(w => w.type));
    if (!workoutTypes.has('stretching') && weekWorkouts.length > 0) {
        recommendations.push({
            type: 'variety',
            message: 'Consider adding a stretching session for recovery.',
            icon: '🧘'
        });
    }

    if (!workoutTypes.has('cardio') && weekWorkouts.length > 2) {
        recommendations.push({
            type: 'variety',
            message: 'Add some cardio to improve your endurance!',
            icon: '🏃'
        });
    }

    return recommendations;
}

module.exports = {
    getProgress,
    recordProgress,
    getStats,
    getDashboard
};
