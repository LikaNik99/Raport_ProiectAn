using System.Configuration;
using Core.Model.Common;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;

namespace Core.Repository.Config
{
    public class AuthorRepository
    {
        private RepositoryContainer Repositories { get; set; }

        public AuthorRepository()
        {
            this.Repositories = RepositoryContainer.Create();
        }

        public async Task<PPResult<string>> AddAuthor(string name)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
               var result = await this.Repositories.Author.AddAuthor__Async(name, CancellationToken.None);

               vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch(Exception ex) 
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "AddAuthor() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<IList<Autori>>> GetAuthors()
        {
            var vResult = new PPResult<IList<Autori>> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Author.GetAuthors__Async(CancellationToken.None);

                if(result.Count > 0)
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
                vResult.ResultMessage = "GetAuthors() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<Autori>> GetAuthor(int id)
        {
            var vResult = new PPResult<Autori> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Author.GetAuthor__Async(id, CancellationToken.None);

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
                vResult.ResultMessage = "GetAuthor() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<string>> UpdateAuthor(int id, string name)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Author.UpdateAuthor__Async(id, name, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.ResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "UpdateAuthor() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<string>> DeleteAuthor(int id)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Author.DeleteAuthor__Async(id, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "DeleteAuthor() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
    }
}
