using System.Security.Cryptography;
using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;
using static Core.Repositories.Infrastructure.Base.StoredProceduresBase;
using static Core.Repositories.Infrastructure.StoredProcedures.book__Schema;

namespace Core.Repositories.Repository.Book
{
    public class AuthorRepository : RepositoryBase<Guid>
    {

        public async Task<SpExecResult<sp_insert_autor__OutParameterValues>> AddAuthor__Async(string pNume, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_insert_autor__Async(cancellationToken, pNume);

            return result;
        }

        public async Task<IList<Autori>> GetAuthors__Async(CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_all_autori__Async(cancellationToken);

            return result;
        }

        public async Task<Autori> GetAuthor__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_author_by_id__Async(cancellationToken, pId);

            return result;
        }

        public async Task<SpExecResult<sp_update_author__OutParameterValues>> UpdateAuthor__Async(int AuthorId, string NewName, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_update_author__Async(cancellationToken, AuthorId, NewName);

            return result;
        }

        public async Task<SpExecResult<sp_delete_author__OutParameterValues>> DeleteAuthor__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_delete_author__Async(cancellationToken, pId);

            return result;
        }
    }
}
