using Core.Model.Common;
using Core.Repositories;

namespace Core.UnitTests
{
    [TestClass]
    public class ServicesRepositoryDbTests
    {
        [TestMethod]
        public async Task AddService()
        {
            // Arrange
            var name = "Jizaa";
            var categoryId = 2;
            var ImageFileName = Guid.NewGuid().ToString();

            // Act
            var userRepository = new ServicesRepository();
            var ppResult = await userRepository.AddService(name, categoryId, ImageFileName);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task UpdateService()
        {
            // Arrange
            var id = 1;
            var name = "Orange";
            var categoryId = 2;
            var ImageFileName = Guid.NewGuid().ToString();

            // Act
            var userRepository = new ServicesRepository();
            var ppResult = await userRepository.UpdateService(id, name, categoryId, ImageFileName);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task GetService()
        {
            // Arrange
            var id = 1;

            // Act
            var userRepository = new ServicesRepository();
            var ppResult = await userRepository.GetService(id);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task GetServices()
        {
            // Arrange

            // Act
            var userRepository = new ServicesRepository();
            var ppResult = await userRepository.GetServices();
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task DeleteService()
        {
            // Arrange
            var id = 1;

            // Act
            var userRepository = new ServicesRepository();
            var ppResult = await userRepository.DeleteService(id);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }
    }
}
