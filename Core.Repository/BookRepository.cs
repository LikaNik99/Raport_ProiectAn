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
    public class BookRepository
    {
        private RepositoryContainer Repositories { get; set; }

        public BookRepository()
        {
            this.Repositories = RepositoryContainer.Create();
        }

        public async Task<PPResult<IList<BookEntity>>> GetBooks()
        {
            var vResult = new PPResult<IList<BookEntity>> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Book.GetBooks__Async(CancellationToken.None);

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

        public async Task<PPResult<BookEntity>> GetBook(int id)
        {
            var vResult = new PPResult<BookEntity> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Book.GetBook__Async(id, CancellationToken.None);

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
                vResult.ResultMessage = "GetCategories() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<string>> UpdateBook(int id, string titlul, int anDePublicare, string imageFileName, int autorId, int categoryId)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Book.UpdateBook__Async(id, titlul, anDePublicare, imageFileName, autorId, categoryId, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "UpdateBook() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<string>> CreateBook(string titlu, int anPublicare, int autorId, int categorieId, string imageFileName)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Book.CreateBook__Async(titlu, anPublicare, autorId, categorieId, imageFileName, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "CreateBook() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<string>> DeleteBook(int id)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Book.DeleteBook__Async(id, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
                if(vResult.ResultCode == PPResponseCode.SUCCESS)
                {
                    vResult.ReturnObject = result.OutputParameterValues.pImageFileName;
                }
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "DeleteBook() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
    }
}
