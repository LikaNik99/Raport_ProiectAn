using Core.Db;
using Core.Utils;
using System.Data.SqlClient;
using System.Security.Cryptography;

namespace Core.Repositories
{
    public static class BalanceRepository
    {
        private static string _connectionBaseDBString = ConfigurationHelper.GetConnectionString("BaseDB")!;

        public static async Task<string> GetBalanceFormatted(int pUserId)
        {
            decimal result = 0.00m;

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pUserId", pUserId)
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT [accounting].[fn_get_user_balance](@pUserId)", parameters, ct), CancellationToken.None);

                if (ds.Tables.Count > 0 && ds.Tables[0].Rows.Count > 0)
                {
                    long value = Convert.ToInt64(ds.Tables[0].Rows[0][0]);
                    result = Math.Round(value / 100m, 2);
                }
            }
            catch (Exception ex)
            {
                var exception = ex.Message;
            }

            return result.ToString("0.00");
        }
    }
}
