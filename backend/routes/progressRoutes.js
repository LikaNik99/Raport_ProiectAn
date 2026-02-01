const express = require('express');
const progressController = require('../controllers/progressController');
const { authMiddleware } = require('../middleware/authMiddleware');

const router = express.Router();

// All routes require authentication
router.use(authMiddleware);

router.get('/', progressController.getProgress);
router.get('/stats', progressController.getStats);
router.get('/dashboard', progressController.getDashboard);
router.post('/', progressController.recordProgress);

module.exports = router;
