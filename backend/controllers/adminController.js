const { User, Workout, Goal, Progress } = require('../models');
const { Op } = require('sequelize');

// Get all users (admin only)
const getUsers = async (req, res) => {
    try {
        const { search, role, isBlocked, limit = 50, offset = 0 } = req.query;

        const where = {};

        if (search) {
            where[Op.or] = [
                { name: { [Op.like]: `%${search}%` } },
                { email: { [Op.like]: `%${search}%` } }
            ];
        }

        if (role) {
            where.role = role;
        }

        if (isBlocked !== undefined) {
            where.isBlocked = isBlocked === 'true';
        }

        const users = await User.findAndCountAll({
            where,
            attributes: { exclude: ['password'] },
            order: [['createdAt', 'DESC']],
            limit: parseInt(limit),
            offset: parseInt(offset)
        });

        res.json({
            success: true,
            data: users.rows,
            pagination: {
                total: users.count,
                limit: parseInt(limit),
                offset: parseInt(offset)
            }
        });
    } catch (error) {
        console.error('Get users error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch users.'
        });
    }
};

// Get single user details
const getUser = async (req, res) => {
    try {
        const user = await User.findByPk(req.params.id, {
            attributes: { exclude: ['password'] }
        });

        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found.'
            });
        }

        // Get user stats
        const workoutCount = await Workout.count({ where: { userId: user.id } });
        const goalCount = await Goal.count({ where: { userId: user.id } });
        const completedGoals = await Goal.count({ where: { userId: user.id, isCompleted: true } });

        res.json({
            success: true,
            data: {
                ...user.toJSON(),
                stats: {
                    workouts: workoutCount,
                    goals: goalCount,
                    completedGoals
                }
            }
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch user.'
        });
    }
};

// Block/unblock user
const toggleBlockUser = async (req, res) => {
    try {
        const user = await User.findByPk(req.params.id);

        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found.'
            });
        }

        if (user.role === 'admin') {
            return res.status(400).json({
                success: false,
                message: 'Cannot block admin users.'
            });
        }

        await user.update({ isBlocked: !user.isBlocked });

        res.json({
            success: true,
            message: user.isBlocked ? 'User blocked successfully.' : 'User unblocked successfully.',
            data: user.toJSON()
        });
    } catch (error) {
        console.error('Toggle block user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update user status.'
        });
    }
};

// Delete user
const deleteUser = async (req, res) => {
    try {
        const user = await User.findByPk(req.params.id);

        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found.'
            });
        }

        if (user.role === 'admin') {
            return res.status(400).json({
                success: false,
                message: 'Cannot delete admin users.'
            });
        }

        await user.destroy();

        res.json({
            success: true,
            message: 'User deleted successfully.'
        });
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete user.'
        });
    }
};

// Get global statistics
const getGlobalStats = async (req, res) => {
    try {
        const totalUsers = await User.count({ where: { role: 'user' } });
        const activeUsers = await User.count({ where: { role: 'user', isBlocked: false } });
        const blockedUsers = await User.count({ where: { isBlocked: true } });
        const totalWorkouts = await Workout.count();
        const totalGoals = await Goal.count();
        const completedGoals = await Goal.count({ where: { isCompleted: true } });

        // Workouts this week
        const startOfWeek = new Date();
        startOfWeek.setDate(startOfWeek.getDate() - startOfWeek.getDay());
        const weeklyWorkouts = await Workout.count({
            where: { workoutDate: { [Op.gte]: startOfWeek.toISOString().split('T')[0] } }
        });

        // New users this month
        const startOfMonth = new Date();
        startOfMonth.setDate(1);
        const newUsersThisMonth = await User.count({
            where: { createdAt: { [Op.gte]: startOfMonth } }
        });

        // Workout types distribution
        const workoutsByType = await Workout.findAll({
            attributes: [
                'type',
                [require('sequelize').fn('COUNT', require('sequelize').col('id')), 'count']
            ],
            group: ['type']
        });

        res.json({
            success: true,
            data: {
                users: {
                    total: totalUsers,
                    active: activeUsers,
                    blocked: blockedUsers,
                    newThisMonth: newUsersThisMonth
                },
                workouts: {
                    total: totalWorkouts,
                    thisWeek: weeklyWorkouts,
                    byType: workoutsByType.reduce((acc, w) => {
                        acc[w.type] = parseInt(w.get('count'));
                        return acc;
                    }, {})
                },
                goals: {
                    total: totalGoals,
                    completed: completedGoals,
                    completionRate: totalGoals ? Math.round((completedGoals / totalGoals) * 100) : 0
                }
            }
        });
    } catch (error) {
        console.error('Get global stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch statistics.'
        });
    }
};

module.exports = {
    getUsers,
    getUser,
    toggleBlockUser,
    deleteUser,
    getGlobalStats
};
