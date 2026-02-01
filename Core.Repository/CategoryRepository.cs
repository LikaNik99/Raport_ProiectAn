using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.Model.Common;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;

namespace Core.Repository.Config
{
    public class CategoryRepository
    {
        private RepositoryContainer Repositories { get; set; }

        public CategoryRepository()
        {
            this.Repositories = RepositoryContainer.Create();
        }

        public async Task<PPResult<string>> AddCategory(string name)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Category.AddCategory__Async(name, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "AddCategory() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<CategoryEntity>> GetCategory(int id)
        {
            var vResult = new PPResult<CategoryEntity> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Category.GetCategory__Async(id, CancellationToken.None);

                if (result != null)
                {
                    vResult.ReturnObject = result;
                    vResult.ResultCode = PPResponseCode.SUCCESS;
                }
                else
                {
                    vResult.ResultCode = PPResponseCode.NOT_FOUND;
                }
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "GetCategory() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<IList<CategoryEntity>>> GetCategories()
        {
            var vResult = new PPResult<IList<CategoryEntity>> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Category.GetCategories__Async(CancellationToken.None);

                if (result.Count > 0)
                {
                    vResult.ResultCode = PPResponseCode.SUCCESS;
                }
                else
                {
                    vResult.ResultCode = PPResponseCode.NOT_FOUND;
                }
                vResult.ReturnObject = result;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "GetCategories() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<string>> UpdateCategory(int id, string name)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Category.UpdateCategory__Async(id, name, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "UpdateCategory() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<string>> DeleteCategory(int id)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Category.DeleteCategory__Async(id, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "DeleteCategory() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
    }
}
