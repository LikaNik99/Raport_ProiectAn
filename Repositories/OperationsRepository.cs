using Core.Db;
using Core.DBModel;
using Core.Model.Common;
using Core.Utils;
using System.Data;
using System.Data.SqlClient;

namespace Core.Repositories
{
    public class OperationsRepository
    {
        private string _connectionBaseDBString;
        public OperationsRepository()
        {
            _connectionBaseDBString = ConfigurationHelper.GetConnectionString("BaseDB")!;
        }

        public async Task<PPResult<IList<EnityOperation>>> GetOperations()
        {
            var result = new PPResult<IList<EnityOperation>> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [checkout].[fn_get_operations]()", new[] { new SqlParameter() }, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EnityOperation>(ds.Tables[0]).OrderByDescending(x => x.Id).ToList();
                    result.ResultCode = PPResponseCode.SUCCESS;
                }
                else
                {
                    result.ResultCode = PPResponseCode.NOT_FOUND;
                }
            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<EnityOperation>> GetOperation(int id)
        {
            var result = new PPResult<EnityOperation> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", id),
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [checkout].[fn_get_operation](@pId)", parameters, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EnityOperation>(ds.Tables[0]).FirstOrDefault();
                    result.ResultCode = PPResponseCode.SUCCESS;
                }
                else
                {
                    result.ResultCode = PPResponseCode.NOT_FOUND;
                }
            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }
    }
}
