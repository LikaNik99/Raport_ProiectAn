const sequelize = require('../config/database');
const User = require('./User');
const Workout = require('./Workout');
const Goal = require('./Goal');
const Progress = require('./Progress');

// Define associations
User.hasMany(Workout, { foreignKey: 'userId', as: 'workouts', onDelete: 'CASCADE' });
Workout.belongsTo(User, { foreignKey: 'userId', as: 'user' });

User.hasMany(Goal, { foreignKey: 'userId', as: 'goals', onDelete: 'CASCADE' });
Goal.belongsTo(User, { foreignKey: 'userId', as: 'user' });

User.hasMany(Progress, { foreignKey: 'userId', as: 'progressRecords', onDelete: 'CASCADE' });
Progress.belongsTo(User, { foreignKey: 'userId', as: 'user' });

module.exports = {
    sequelize,
    User,
    Workout,
    Goal,
    Progress
};
