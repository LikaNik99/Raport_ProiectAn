using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;
using static Core.Repositories.Infrastructure.Base.StoredProceduresBase;
using static Core.Repositories.Infrastructure.StoredProcedures.book__Schema;

namespace Core.Repositories.Repository.Book
{
    public class CategoryRepository : RepositoryBase<Guid>
    {
        public async Task<SpExecResult<sp_add_categorie__OutParameterValues>> AddCategory__Async(string pNume, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_add_categorie__Async(cancellationToken, pNume);

            return result;
        }
        public async Task<CategoryEntity> GetCategory__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_categorie_by_id__Async(cancellationToken, pId);

            return result;
        }
        public async Task<IList<CategoryEntity>> GetCategories__Async(CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.TableValuedFunctions.book.fn_get_categorii__Async(cancellationToken);

            return result;
        }

        public async Task<SpExecResult<sp_update_categorie__OutParameterValues>> UpdateCategory__Async(int pId, string pNume, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_update_categorie__Async(cancellationToken, pId, pNume);

            return result;
        }

        public async Task<SpExecResult<sp_delete_categorie__OutParameterValues>> DeleteCategory__Async(int pId, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.book.sp_delete_categorie__Async(cancellationToken, pId);

            return result;
        }
    }
}
