const { Goal } = require('../models');

// Get all goals for user
const getGoals = async (req, res) => {
    try {
        const { isCompleted } = req.query;

        const where = { userId: req.userId };

        if (isCompleted !== undefined) {
            where.isCompleted = isCompleted === 'true';
        }

        const goals = await Goal.findAll({
            where,
            order: [['deadline', 'ASC'], ['createdAt', 'DESC']]
        });

        res.json({
            success: true,
            data: goals
        });
    } catch (error) {
        console.error('Get goals error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch goals.'
        });
    }
};

// Get single goal
const getGoal = async (req, res) => {
    try {
        const goal = await Goal.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!goal) {
            return res.status(404).json({
                success: false,
                message: 'Goal not found.'
            });
        }

        res.json({
            success: true,
            data: goal
        });
    } catch (error) {
        console.error('Get goal error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch goal.'
        });
    }
};

// Create new goal
const createGoal = async (req, res) => {
    try {
        const { goalType, targetValue, targetUnit, currentValue, deadline, description } = req.body;

        const goal = await Goal.create({
            userId: req.userId,
            goalType,
            targetValue,
            targetUnit,
            currentValue: currentValue || 0,
            deadline,
            description
        });

        res.status(201).json({
            success: true,
            message: 'Goal created successfully.',
            data: goal
        });
    } catch (error) {
        console.error('Create goal error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to create goal.'
        });
    }
};

// Update goal
const updateGoal = async (req, res) => {
    try {
        const goal = await Goal.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!goal) {
            return res.status(404).json({
                success: false,
                message: 'Goal not found.'
            });
        }

        const { goalType, targetValue, targetUnit, currentValue, deadline, description, isCompleted } = req.body;

        await goal.update({
            goalType: goalType || goal.goalType,
            targetValue: targetValue !== undefined ? targetValue : goal.targetValue,
            targetUnit: targetUnit || goal.targetUnit,
            currentValue: currentValue !== undefined ? currentValue : goal.currentValue,
            deadline: deadline !== undefined ? deadline : goal.deadline,
            description: description !== undefined ? description : goal.description,
            isCompleted: isCompleted !== undefined ? isCompleted : goal.isCompleted
        });

        res.json({
            success: true,
            message: 'Goal updated successfully.',
            data: goal
        });
    } catch (error) {
        console.error('Update goal error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update goal.'
        });
    }
};

// Delete goal
const deleteGoal = async (req, res) => {
    try {
        const goal = await Goal.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!goal) {
            return res.status(404).json({
                success: false,
                message: 'Goal not found.'
            });
        }

        await goal.destroy();

        res.json({
            success: true,
            message: 'Goal deleted successfully.'
        });
    } catch (error) {
        console.error('Delete goal error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete goal.'
        });
    }
};

// Update goal progress
const updateGoalProgress = async (req, res) => {
    try {
        const goal = await Goal.findOne({
            where: { id: req.params.id, userId: req.userId }
        });

        if (!goal) {
            return res.status(404).json({
                success: false,
                message: 'Goal not found.'
            });
        }

        const { currentValue } = req.body;

        const isCompleted = currentValue >= goal.targetValue;

        await goal.update({
            currentValue,
            isCompleted
        });

        res.json({
            success: true,
            message: isCompleted ? 'Congratulations! Goal completed!' : 'Progress updated.',
            data: goal
        });
    } catch (error) {
        console.error('Update goal progress error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update progress.'
        });
    }
};

module.exports = {
    getGoals,
    getGoal,
    createGoal,
    updateGoal,
    deleteGoal,
    updateGoalProgress
};
