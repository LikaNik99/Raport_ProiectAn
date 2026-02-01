const express = require('express');
const workoutController = require('../controllers/workoutController');
const { authMiddleware } = require('../middleware/authMiddleware');

const router = express.Router();

// All routes require authentication
router.use(authMiddleware);

router.get('/', workoutController.getWorkouts);
router.get('/stats', workoutController.getWorkoutStats);
router.get('/:id', workoutController.getWorkout);
router.post('/', workoutController.createWorkout);
router.put('/:id', workoutController.updateWorkout);
router.delete('/:id', workoutController.deleteWorkout);

module.exports = router;
