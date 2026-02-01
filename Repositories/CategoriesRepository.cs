using System.Data;
using System.Data.SqlClient;
using Core.Db;
using Core.DBModel;
using Core.Model.Common;
using Core.Utils;

namespace Core.Repositories
{
    public class CategoriesRepository
    {
        private string _connectionBaseDBString;
        public CategoriesRepository()
        {
            _connectionBaseDBString = ConfigurationHelper.GetConnectionString("BaseDB")!;
        }

        public async Task<PPResult<string>> AddCategory(string pCategoryName)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pName", pCategoryName),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_category_add]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<string>> UpdateCategory(int pId, string pCategoryName)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId),
                    new SqlParameter("@pName", pCategoryName),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_update_categorie]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<string>> DeleteCategory(int pId)
        {
            var result = new PPResult<string> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId),
                    new SqlParameter("@pResponseCode", SqlDbType.Int) {Direction = ParameterDirection.Output}
                };

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.StoredProcOpenAsync(@"[config].[sp_category_delete]", parameters, ct), CancellationToken.None);

                result.ResultCode = (PPResponseCode)parameters.FirstOrDefault(x => x.ParameterName == "@pResponseCode")!.Value;

            }
            catch (Exception ex)
            {
                result.ResultCode = PPResponseCode.CRITICAL_ERROR;
                result.ResultMessage = ex.Message;
            }

            return result;
        }

        public async Task<PPResult<IList<EntityCategory>>> GetCategories()
        {
            var result = new PPResult<IList<EntityCategory>> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {

                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [config].[fn_get_categories]()", new[] { new SqlParameter() }, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EntityCategory>(ds.Tables[0]);
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

        public async Task<PPResult<EntityCategory>> GetCategory(int pId)
        {
            var result = new PPResult<EntityCategory> { ResultCode = PPResponseCode.TECHNICAL_ERROR };

            try
            {
                var parameters = new SqlParameter[]
                {
                    new SqlParameter("@pId", pId)
                };
                var ds = await new Session(_connectionBaseDBString).SqlAsync((session, ct)
                    => session.SQLTextOpenAsync(@"SELECT * FROM [config].[fn_get_category_by_id](@pId)", parameters, ct), CancellationToken.None);


                if (ds.Tables.Count > 0)
                {
                    result.ReturnObject = PPDataTableExtensions.ToList<EntityCategory>(ds.Tables[0]).FirstOrDefault();
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
