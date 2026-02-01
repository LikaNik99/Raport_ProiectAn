#pragma warning disable IDE1006 // Naming Styles
using Core.Db;
using Core.Repository.Config.DBModel;

namespace Core.Repositories.Infrastructure
{
    public class TableValuedFunctions : Base.TableValuedFunctionsBase
    {
        public TableValuedFunctions(Session session) : base(session) { }

        public book__Schema book { get; protected set; }

        public class book__Schema : SchemaBase
        {
            public book__Schema(Session session, TableValuedFunctions storedProcedures) : base(session, storedProcedures) { }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_all_autori] ()</code>
			/// </summary>
			public async Task<IList<Autori>> fn_get_all_autori__Async(CancellationToken cancellationToken)
            {
                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_all_autori] ()", cancellationToken);
                var result = ToList<Autori>(dt);
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_categorii] ()</code>
			/// </summary>
			public async Task<IList<CategoryEntity>> fn_get_categorii__Async(CancellationToken cancellationToken)
            {
                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_categorii] ()", cancellationToken);
                var result = ToList<CategoryEntity>(dt);
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_categorie_by_id] (@pId)</code>
			/// </summary>
			public async Task<CategoryEntity> fn_get_categorie_by_id__Async(CancellationToken cancellationToken, int pId)
            {
                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                };

                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_categorie_by_id] (@pId)", parameters, cancellationToken);
                var result = ToList<CategoryEntity>(dt).FirstOrDefault();
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_books] ()</code>
			/// </summary>
			public async Task<IList<BookEntity>> fn_get_books__Async(CancellationToken cancellationToken)
            {
                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_books] ()", cancellationToken);
                var result = ToList<BookEntity>(dt);
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_carte_by_id](@pId)</code>
			/// </summary>
			public async Task<BookEntity> fn_get_carte_by_id__Async(CancellationToken cancellationToken, int pId)
            {
                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                };

                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_carte_by_id] (@pId)", parameters, cancellationToken);
                var result = ToList<BookEntity>(dt).FirstOrDefault();
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_author_by_id] (@pId)</code>
			/// </summary>
			public async Task<Autori> fn_get_author_by_id__Async(CancellationToken cancellationToken, int pId)
            {
                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                };

                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_author_by_id] (@pId)", parameters, cancellationToken);
                var result = ToList<Autori>(dt).FirstOrDefault();
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_recenzii] ()</code>
			/// </summary>
			public async Task<IList<Review>> fn_get_recenzii__Async(CancellationToken cancellationToken)
            {
                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_recenzii] ()", cancellationToken);
                var result = ToList<Review>(dt);
                return result;
            }

            /// <summary>
			/// Table-valued function
			/// <code>[book].[fn_get_review_by_id] (@pId)</code>
			/// </summary>
			public async Task<Review> fn_get_review_by_id__Async(CancellationToken cancellationToken, int pId)
            {
                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                };

                var dt = await this.TableValuedFunctions.SelectTableFromFunctionAsync("[book].[fn_get_review_by_id] (@pId)", parameters, cancellationToken);
                var result = ToList<Review>(dt).FirstOrDefault();
                return result;
            }
        }
    }
}
