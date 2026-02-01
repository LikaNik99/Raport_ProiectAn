using System.Data;
using System.Data.SqlClient;
using System.Security.Cryptography;
using System.Text;
using Core.Db;
using Core.DBModel;
using Core.Model.Common;
using Core.Utils;

namespace Core.Repositories
{
    public class UsersReposiotry
    {
        private static readonly string SecretKey = "SuperSecretKey123";
        private string _connectionBaseDBString;

        public UsersReposiotry()
        {
            _connectionBaseDBString = ConfigurationHelper.GetConnectionString("BaseDB")!;
        }

        public async Task<PPResult<string>> AddUser(string pUsername, string pEmail, string pMobile, string password)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var passwordHash = HashPassword(password);
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pUsername", pUsername),
                    new SqlParameter("@pEmail", pEmail),
                    new SqlParameter("@pMobile", pMobile),
                    new SqlParameter("@pPasswordHash", passwordHash),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct) 
                    => session.StoredProcOpenAsync(@"[auth].[sp_entity_user_create]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<EntityUser>> Login(string pUserEmail, string password)
        {
            var result = new PPResult<EntityUser> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var passwordHash = HashPassword(password);
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pUserEmail", pUserEmail),
                    new SqlParameter("@pPasswordHash", passwordHash),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[auth].[sp_entity_user_login]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

                if (result.ResultCode == PPResponseCode.SUCCESS)
                {
                    if (ds.Tables.Count > 0)
                    {
                        result.ReturnObject = PPDataTableExtensions.ToList<EntityUser>(ds.Tables[0]).FirstOrDefault();
                    }
                }

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        private static string HashPassword(string password)
        {
            byte[] bytes;
            using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(SecretKey)))
            {
                bytes = hmac.ComputeHash(Encoding.UTF8.GetBytes(password));
            }
            return BitConverter.ToString(bytes).Replace("-", "").ToLower();
        }
    }
}
