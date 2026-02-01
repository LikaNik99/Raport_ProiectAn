const express = require('express');
const adminController = require('../controllers/adminController');
const { authMiddleware } = require('../middleware/authMiddleware');
const { isAdmin } = require('../middleware/roleMiddleware');

const router = express.Router();

// All routes require authentication and admin role
router.use(authMiddleware);
router.use(isAdmin);

router.get('/users', adminController.getUsers);
router.get('/users/:id', adminController.getUser);
router.put('/users/:id/block', adminController.toggleBlockUser);
router.delete('/users/:id', adminController.deleteUser);
router.get('/stats', adminController.getGlobalStats);

module.exports = router;
