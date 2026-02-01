using Core.Model.Common;
using Core.Repositories;

namespace Core.UnitTests
{
    [TestClass]
    public class CategoriesRepositoryDbTest
    {
        [TestMethod]
        public async Task AddCategory()
        {
            // Arrange
            var name = "Jizaa";

            // Act
            var userRepository = new CategoriesRepository();
            var ppResult = await userRepository.AddCategory(name);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task UpdateCategory()
        {
            // Arrange
            var name = "Telefonie Mobila";
            var id = 2;

            // Act
            var userRepository = new CategoriesRepository();
            var ppResult = await userRepository.UpdateCategory(id, name);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task GetCategory()
        {
            // Arrange
            var id = 1;

            // Act
            var userRepository = new CategoriesRepository();
            var ppResult = await userRepository.GetCategory(id);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task GetCategories()
        {
            // Arrange

            // Act
            var userRepository = new CategoriesRepository();
            var ppResult = await userRepository.GetCategories();
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task DeleteCategory()
        {
            // Arrange
            var id = 1;

            // Act
            var userRepository = new CategoriesRepository();
            var ppResult = await userRepository.DeleteCategory(id);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }
    }
}
