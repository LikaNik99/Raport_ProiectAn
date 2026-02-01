using Core.Model.Common;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;

namespace Core.Repository.Config
{
    public class ReviewRepository
    {
        private RepositoryContainer Repositories { get; set; }

        public ReviewRepository()
        {
            this.Repositories = RepositoryContainer.Create();
        }

        public async Task<PPResult<IList<Review>>> GetReviews()
        {
            var vResult = new PPResult<IList<Review>> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Review.GetReviews__Async(CancellationToken.None);

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
                vResult.ResultMessage = "GetReviews() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<Review>> GetReview(int id)
        {
            var vResult = new PPResult<Review> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Review.GetReview__Async(id, CancellationToken.None);

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
                vResult.ResultMessage = "GetReview() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
        public async Task<PPResult<string>> AddReview(string recenzor, string mesaj, int bookId)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Review.AddReview__Async(recenzor, mesaj, bookId, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "AddReview() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<string>> UpdateReview(int id, string recenzor, string mesaj, int carteId)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Review.UpdateReview__Async(id, recenzor, mesaj, carteId, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "UpdateReview() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }

        public async Task<PPResult<string>> DeleteReview(int id)
        {
            var vResult = new PPResult<string> { ResultCode = PPResponseCode.CRITICAL_ERROR };

            try
            {
                var result = await this.Repositories.Review.DeleteReview__Async(id, CancellationToken.None);

                vResult.ResultCode = result.OutputParameterValues.pResponseCode;
            }
            catch (Exception ex)
            {
                vResult.ResultCode = PPResponseCode.TECHNICAL_ERROR;
                vResult.ResultMessage = "DeleteReview() failed";
                vResult.ResultPrivateMessage = ex.Message;
            }
            return vResult;
        }
    }
}
