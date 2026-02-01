const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Goal = sequelize.define('Goal', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    userId: {
        type: DataTypes.INTEGER,
        allowNull: false,
        references: {
            model: 'users',
            key: 'id'
        }
    },
    goalType: {
        type: DataTypes.ENUM('weight_loss', 'muscle_gain', 'endurance', 'workouts_per_week', 'steps', 'custom'),
        allowNull: false
    },
    targetValue: {
        type: DataTypes.FLOAT,
        allowNull: false
    },
    targetUnit: {
        type: DataTypes.STRING,
        allowNull: false,
        comment: 'kg, minutes, steps, workouts, etc.'
    },
    currentValue: {
        type: DataTypes.FLOAT,
        defaultValue: 0
    },
    deadline: {
        type: DataTypes.DATEONLY,
        allowNull: true
    },
    isCompleted: {
        type: DataTypes.BOOLEAN,
        defaultValue: false
    },
    description: {
        type: DataTypes.TEXT,
        allowNull: true
    }
}, {
    tableName: 'goals'
});

module.exports = Goal;
