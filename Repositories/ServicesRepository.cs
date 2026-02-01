using System.Data;
using System.Data.SqlClient;
using Core.Db;
using Core.DBModel;
using Core.Model.Common;
using Core.Utils;

namespace Core.Repositories
{
    public class ServicesRepository
    {
        private string _connectionBaseDBString;

        public ServicesRepository()
        {
            _connectionBaseDBString = ConfigurationHelper.GetConnectionString("BaseDB")!;
        }

        public async Task<PPResult<string>> AddService(string pName, int pCategoryId, string pImageFileName)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pName", pName),
                    new SqlParameter("@pCategoryId", pCategoryId),
                    new SqlParameter("@pImageFileName", pImageFileName),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_service_add]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<string>> UpdateService(int pId, string pName, int pCategoryId, string pImageFileName)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId),
                    new SqlParameter("@pName", pName),
                    new SqlParameter("@pCategoryId", pCategoryId),
                    new SqlParameter("@pImageFileName", pImageFileName),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_service_update]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<string>> DeleteService(int pId)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output},
                    new SqlParameter("@pImageFileName", SqlDbType.NVarChar) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_service_delete]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;
                result.ReturnObject = (string)parameters.FirstOrDefault(x => x.ParameterName == "@pImageFileName")!.Value;
            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }
        public async Task<PPResult<int>> PayService(int pUserId, long pAmount, int pServiceId, int pProviderId)
        {
            var result = new PPResult<int> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pUserId", pUserId),
                    new SqlParameter("@pServiceId", pServiceId),
                    new SqlParameter("@pAmount", pAmount),
                    new SqlParameter("@pProviderId", pProviderId),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output},
                    new SqlParameter("@pOpId", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[checkout].[sp_service_pay]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;
                result.ReturnObject = (int)parameters.FirstOrDefault(x => x.ParameterName == "@pOpId")!.Value;
            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<int>> TopUpPay(int pUserId, long pAmount)
        {
            var result = new PPResult<int> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pUserId", pUserId),
                    new SqlParameter("@pAmount", pAmount),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output},
                    new SqlParameter("@pOpId", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[checkout].[sp_topup_pay]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;
                result.ReturnObject = (int)parameters.FirstOrDefault(x => x.ParameterName == "@pOpId")!.Value;
            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<IList<EntityService>>> GetServices()
        {
            var result = new PPResult<IList<EntityService>> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [config].[fn_get_services]()", new[] { new SqlParameter() }, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EntityService>(ds.Tables[0]);
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

        public async Task<PPResult<EntityService>> GetService(int pId)
        {
            var result = new PPResult<EntityService> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId)
                };
                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [config].[fn_get_service_by_id](@pId)", parameters, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EntityService>(ds.Tables[0]).FirstOrDefault();
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
