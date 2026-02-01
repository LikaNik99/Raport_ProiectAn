const { Workout } = require('../models');
const { Op } = require('sequelize');

// Get all workouts for user
const getWorkouts = async (req, res) => {
    try {
        const { type, startDate, endDate, limit = 50, offset = 0 } = req.query;

        const where = { userId: req.userId };

        if (type) {
            where.type = type;
        }

        if (startDate || endDate) {
            where.workoutDate = {};
            if (startDate) where.workoutDate[Op.gte] = startDate;
            if (endDate) where.workoutDate[Op.lte] = endDate;
        }

        const workouts = await Workout.findAndCountAll({
            where,
            order: [['workoutDate', 'DESC'], ['createdAt', 'DESC']],
            limit: parseInt(limit),
            offset: parseInt(offset)
        });

        res.json({
            success: true,
            data: workouts.rows,
            pagination: {
                total: workouts.count,
                limit: parseInt(limit),
                offset: parseInt(offset)
            }
        });
    } catch (error) {
        console.error('Get workouts error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch workouts.'
        });
    }
};

// Get single workout
const getWorkout = async (req, res) => {
    try {
        const workout = await Workout.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!workout) {
            return res.status(404).json({
                success: false,
                message: 'Workout not found.'
            });
        }

        res.json({
            success: true,
            data: workout
        });
    } catch (error) {
        console.error('Get workout error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch workout.'
        });
    }
};

// Create new workout
const createWorkout = async (req, res) => {
    try {
        const { type, durationMinutes, caloriesBurned, workoutDate, notes } = req.body;

        const workout = await Workout.create({
            userId: req.userId,
            type,
            durationMinutes,
            caloriesBurned,
            workoutDate: workoutDate || new Date().toISOString().split('T')[0],
            notes
        });

        res.status(201).json({
            success: true,
            message: 'Workout logged successfully.',
            data: workout
        });
    } catch (error) {
        console.error('Create workout error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to log workout.'
        });
    }
};

// Update workout
const updateWorkout = async (req, res) => {
    try {
        const workout = await Workout.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!workout) {
            return res.status(404).json({
                success: false,
                message: 'Workout not found.'
            });
        }

        const { type, durationMinutes, caloriesBurned, workoutDate, notes } = req.body;

        await workout.update({
            type: type || workout.type,
            durationMinutes: durationMinutes || workout.durationMinutes,
            caloriesBurned: caloriesBurned !== undefined ? caloriesBurned : workout.caloriesBurned,
            workoutDate: workoutDate || workout.workoutDate,
            notes: notes !== undefined ? notes : workout.notes
        });

        res.json({
            success: true,
            message: 'Workout updated successfully.',
            data: workout
        });
    } catch (error) {
        console.error('Update workout error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update workout.'
        });
    }
};

// Delete workout
const deleteWorkout = async (req, res) => {
    try {
        const workout = await Workout.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!workout) {
            return res.status(404).json({
                success: false,
                message: 'Workout not found.'
            });
        }

        await workout.destroy();

        res.json({
            success: true,
            message: 'Workout deleted successfully.'
        });
    } catch (error) {
        console.error('Delete workout error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete workout.'
        });
    }
};

// Get workout statistics
const getWorkoutStats = async (req, res) => {
    try {
        const { startDate, endDate } = req.query;

        const where = { userId: req.userId };

        if (startDate || endDate) {
            where.workoutDate = {};
            if (startDate) where.workoutDate[Op.gte] = startDate;
            if (endDate) where.workoutDate[Op.lte] = endDate;
        }

        const workouts = await Workout.findAll({ where });

        const stats = {
            totalWorkouts: workouts.length,
            totalDuration: workouts.reduce((sum, w) => sum + w.durationMinutes, 0),
            totalCalories: workouts.reduce((sum, w) => sum + (w.caloriesBurned || 0), 0),
            byType: {}
        };

        workouts.forEach(w => {
            if (!stats.byType[w.type]) {
                stats.byType[w.type] = { count: 0, duration: 0, calories: 0 };
            }
            stats.byType[w.type].count++;
            stats.byType[w.type].duration += w.durationMinutes;
            stats.byType[w.type].calories += w.caloriesBurned || 0;
        });

        res.json({
            success: true,
            data: stats
        });
    } catch (error) {
        console.error('Get workout stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch workout statistics.'
        });
    }
};

module.exports = {
    getWorkouts,
    getWorkout,
    createWorkout,
    updateWorkout,
    deleteWorkout,
    getWorkoutStats
};
