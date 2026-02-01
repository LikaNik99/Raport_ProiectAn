const express = require('express');
const goalController = require('../controllers/goalController');
const { authMiddleware } = require('../middleware/authMiddleware');

const router = express.Router();

// All routes require authentication
router.use(authMiddleware);

router.get('/', goalController.getGoals);
router.get('/:id', goalController.getGoal);
router.post('/', goalController.createGoal);
router.put('/:id', goalController.updateGoal);
router.put('/:id/progress', goalController.updateGoalProgress);
router.delete('/:id', goalController.deleteGoal);

module.exports = router;
