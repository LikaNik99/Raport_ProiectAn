using System.Security.Cryptography;
using System.Text;
using Core.Model.Common;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;

namespace Core.Repository.Config
{
    public class UserRepository
    {
        private static readonly string SecretKey = "SuperSecretKey123";
        private RepositoryContainer Repositories { get; set; }

        public UserRepository()
        {
            this.Repositories = RepositoryContainer.Create();
        }

        public async Task<PPResult<string>> AddUser(string username, string password, string email, string mobile)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var passwordHash = HashPassword(password);
                var result = await this.Repositories.User.CreateUser__Async(username, email, mobile, passwordHash, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "AddAuthor() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<UserEntity>> UserLogin(string username, string password)
        {
            var vResult = new PPResult<UserEntity> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var passwordHash = HashPassword(password);
                var result = await this.Repositories.User.UserLogin__Async(username, passwordHash, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
                vResult.ReturnObject = result.Value;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "UserLogin() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        private static string HashPassword(string password)
        {
            byte[] bytes; 
            using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(SecretKey)))
            {
                bytes =  hmac.ComputeHash(Encoding.UTF8.GetBytes(password));
            }
            return BitConverter.ToString(bytes).Replace("-", "").ToLower();
        }
    }
}
