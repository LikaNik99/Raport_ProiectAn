using System.Security.Cryptography;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;
using static Core.Repositories.Infrastructure.Base.StoredProceduresBase;
using static Core.Repositories.Infrastructure.StoredProcedures.book__Schema;

namespace Core.Repositories.Repository.Book
{
    public class ReviewRepository : RepositoryBase<Guid>
    {

        public async Task<IList<Review>> GetReviews__Async(CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_recenzii__Async(cancellationToken);

            return result;
        }

        public async Task<Review> GetReview__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_review_by_id__Async(cancellationToken, pId);

            return result;
        }

        public async Task<SpExecResult<sp_add_review__OutParameterValues>> AddReview__Async(string pRecenzor, string pMesaj, int pCarteId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_add_review__Async(cancellationToken, pRecenzor, pMesaj, pCarteId);

            return result;
        }

        public async Task<SpExecResult<sp_update_review__OutParameterValues>> UpdateReview__Async(int pId, string pRecenzor, string pMesaj, int pCarteId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_update_review__Async(cancellationToken, pId, pRecenzor, pMesaj, pCarteId);

            return result;
        }

        public async Task<SpExecResult<sp_delete_review__OutParameterValues>> DeleteReview__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_delete_review__Async(cancellationToken, pId);

            return result;
        }
    }
}
