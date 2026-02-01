using Core.Model.Common;
using Core.Repositories;

namespace Core.UnitTests
{
    [TestClass]
    public class UsersReposiotryDbTests
    {

        [TestMethod]
        public async Task AddUser()
        {
            // Arrange
            var userName = "Jizaa";
            var email = "10n7@bk.ru";
            var mobile = "37369783513";
            var password = "test001";

            // Act
            var userRepository = new UsersReposiotry();
            var ppResult = await userRepository.AddUser(userName, email, mobile, password);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }

        [TestMethod]
        public async Task Login()
        {
            // Arrange
            var userName = "Jizaa";
            var password = "test001";

            // Act
            var userRepository = new UsersReposiotry();
            var ppResult = await userRepository.Login(userName, password);
            var result = ppResult.ReturnObject;

            // Assert
            Assert.AreEqual(ppResult.ResultCode, PPResponseCode.SUCCESS);
        }
    }
}
