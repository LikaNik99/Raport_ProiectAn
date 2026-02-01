const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Progress = sequelize.define('Progress', {
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
    weight: {
        type: DataTypes.FLOAT,
        allowNull: true,
        comment: 'Weight in kg'
    },
    recordDate: {
        type: DataTypes.DATEONLY,
        allowNull: false,
        defaultValue: DataTypes.NOW
    },
    totalWorkouts: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    totalCalories: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    totalDuration: {
        type: DataTypes.INTEGER,
        defaultValue: 0,
        comment: 'Total workout duration in minutes'
    },
    notes: {
        type: DataTypes.TEXT,
        allowNull: true
    }
}, {
    tableName: 'progress'
});

module.exports = Progress;
