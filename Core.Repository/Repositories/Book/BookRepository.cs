using System.Security.Cryptography;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;
using static Core.Repositories.Infrastructure.Base.StoredProceduresBase;
using static Core.Repositories.Infrastructure.StoredProcedures.book__Schema;

namespace Core.Repositories.Repository.Book
{
    public class BookRepository : RepositoryBase<Guid>
    {
        public async Task<IList<BookEntity>> GetBooks__Async(CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_books__Async(cancellationToken);

            return result;
        }

        public async Task<SpExecResult<sp_update_carte__OutParameterValues>> UpdateBook__Async(int pId, string pTitlu, int pAnPublicare, string pImageFileName, int pAutorId, int pCategorieId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_update_carte__Async(cancellationToken, pId, pTitlu, pAnPublicare, pImageFileName, pAutorId, pCategorieId);

            return result;
        }

        public async Task<BookEntity> GetBook__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_carte_by_id__Async(cancellationToken, pId);

            return result;
        }

        public async Task<SpExecResult<sp_delete_carte__OutParameterValues>> DeleteBook__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_delete_carte__Async(cancellationToken, pId);

            return result;
        }

        public async Task<SpExecResult<sp_create_carte__OutParameterValues>> CreateBook__Async(string pTitlu, int pAnPublicare, int pAutorId, int pCategorieId, string pImageFileName, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_create_carte__Async(cancellationToken, pTitlu, pAnPublicare, pAutorId, pCategorieId, pImageFileName);

            return result;
        }
    }
}
