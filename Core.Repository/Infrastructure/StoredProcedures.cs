using System.Data;
using Core.Db;
using Core.Model.Common;
using Core.Repository.Config.DBModel;

namespace Core.Repositories.Infrastructure
{
    public class StoredProcedures : Base.StoredProceduresBase
    {
        public StoredProcedures(Session session) : base(session) { }

        /// <summary>Schema <c>[book]</c></summary>
        public book__Schema book { get; protected set; }

        public class book__Schema : SchemaBase
        {
            public book__Schema(Session session, StoredProcedures storedProcedures) : base(session, storedProcedures) { }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_insert_autor]</code>
			/// </summary>
			public async Task<SpExecResult<sp_insert_autor__OutParameterValues>> sp_insert_autor__Async(CancellationToken cancellationToken, string pNume)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pNume", pNume),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_insert_autor]", parameters, cancellationToken);
                var outParameterValues = new sp_insert_autor__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_insert_autor__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_add_categorie]</code>
			/// </summary>
			public async Task<SpExecResult<sp_add_categorie__OutParameterValues>> sp_add_categorie__Async(CancellationToken cancellationToken, string pNume)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pNume", pNume),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_add_categorie]", parameters, cancellationToken);
                var outParameterValues = new sp_add_categorie__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_add_categorie__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_update_categorie]</code>
			/// </summary>
			public async Task<SpExecResult<sp_update_categorie__OutParameterValues>> sp_update_categorie__Async(CancellationToken cancellationToken, int pId, string pNume)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    CreateParameter("@pNume", pNume),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_update_categorie]", parameters, cancellationToken);
                var outParameterValues = new sp_update_categorie__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_update_categorie__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
            /// Stored procedure
            /// <code>[book].[sp_delete_categorie]</code>
            /// </summary>
            public async Task<SpExecResult<sp_delete_categorie__OutParameterValues>> sp_delete_categorie__Async(CancellationToken cancellationToken, int pId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_delete_categorie]", parameters, cancellationToken);
                var outParameterValues = new sp_delete_categorie__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_delete_categorie__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_update_carte]</code>
			/// </summary>
			public async Task<SpExecResult<sp_update_carte__OutParameterValues>> sp_update_carte__Async(CancellationToken cancellationToken, int pId, string pTitlu, int pAnPublicare, string pImageFileName, int pAutorId, int pCategorieId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    CreateParameter("@pTitlu", pTitlu),
                    CreateParameter("@pAnPublicare", pAnPublicare),
                    CreateParameter("@pImageFileName", pImageFileName),
                    CreateParameter("@pAutorId", pAutorId),
                    CreateParameter("@pCategorieId", pCategorieId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_update_carte]", parameters, cancellationToken);
                var outParameterValues = new sp_update_carte__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_update_carte__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
            /// Stored procedure
            /// <code>[book].[sp_delete_carte]</code>
            /// </summary>
            public async Task<SpExecResult<sp_delete_carte__OutParameterValues>> sp_delete_carte__Async(CancellationToken cancellationToken, int pId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);
                var imageFileNameParameter = CreateOutputParameter("@pImageFileName", SqlDbType.NVarChar);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    pResponseCodeParameter,
                    imageFileNameParameter
                };

                await this.StoredProcedures.SelectTableFromStoredProcedureAsync("[book].[sp_delete_carte]", parameters, cancellationToken);
                var outParameterValues = new sp_delete_carte__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                outParameterValues.pImageFileName = ToValueString(imageFileNameParameter);
                return ExecResult(outParameterValues);
            }

            public class sp_delete_carte__OutParameterValues
            {
                public PPResponseCode pResponseCode;
                public string pImageFileName;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_create_carte]</code>
			/// </summary>
			public async Task<SpExecResult<sp_create_carte__OutParameterValues>> sp_create_carte__Async(CancellationToken cancellationToken, string pTitlu, int pAnPublicare, int pAutorId, int pCategorieId, string pImageFileName)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pTitlu", pTitlu),
                    CreateParameter("@pAnPublicare", pAnPublicare),
                    CreateParameter("@pAutorId", pAutorId),
                    CreateParameter("@pCategorieId", pCategorieId),
                    CreateParameter("@pImageFileName", pImageFileName),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_create_carte]", parameters, cancellationToken);
                var outParameterValues = new sp_create_carte__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_create_carte__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_update_author]</code>
			/// </summary>
			public async Task<SpExecResult<sp_update_author__OutParameterValues>> sp_update_author__Async(CancellationToken cancellationToken, int AuthorId, string NewName)
            {
                var ResponseCodeParameter = CreateOutputParameter("@ResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@AuthorId", AuthorId),
                    CreateParameter("@NewName", NewName),
                    ResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_update_author]", parameters, cancellationToken);
                var outParameterValues = new sp_update_author__OutParameterValues();
                outParameterValues.ResponseCode = ToValue<PPResponseCode>(ResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_update_author__OutParameterValues
            {
                public PPResponseCode ResponseCode;
            }

            /// <summary>
            /// Stored procedure
            /// <code>[book].[sp_delete_author]</code>
            /// </summary>
            public async Task<SpExecResult<sp_delete_author__OutParameterValues>> sp_delete_author__Async(CancellationToken cancellationToken, int pId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_delete_author]", parameters, cancellationToken);
                var outParameterValues = new sp_delete_author__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }
            public class sp_delete_author__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_add_review]</code>
			/// </summary>
			public async Task<SpExecResult<sp_add_review__OutParameterValues>> sp_add_review__Async(CancellationToken cancellationToken, string pRecenzor, string pMesaj, int pCarteId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pRecenzor", pRecenzor),
                    CreateParameter("@pMesaj", pMesaj),
                    CreateParameter("@pCarteId", pCarteId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_add_review]", parameters, cancellationToken);
                var outParameterValues = new sp_add_review__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_add_review__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_update_review]</code>
			/// </summary>
			public async Task<SpExecResult<sp_update_review__OutParameterValues>> sp_update_review__Async(CancellationToken cancellationToken, int pId, string pRecenzor, string pMesaj, int pCarteId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    CreateParameter("@pRecenzor", pRecenzor),
                    CreateParameter("@pMesaj", pMesaj),
                    CreateParameter("@pCarteId", pCarteId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_update_review]", parameters, cancellationToken);
                var outParameterValues = new sp_update_review__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_update_review__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[book].[sp_delete_review]</code>
			/// </summary>
			public async Task<SpExecResult<sp_delete_review__OutParameterValues>> sp_delete_review__Async(CancellationToken cancellationToken, int pId)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pId", pId),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[book].[sp_delete_review]", parameters, cancellationToken);
                var outParameterValues = new sp_delete_review__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_delete_review__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }
        }

        /// <summary>Schema <c>[auth]</c></summary>
        public auth__Schema auth { get; protected set; }

        public class auth__Schema : SchemaBase
        {
            public auth__Schema(Session session, StoredProcedures storedProcedures) : base(session, storedProcedures) { }

            /// <summary>
			/// Stored procedure
			/// <code>[auth].[sp_user_create]</code>
			/// </summary>
			public async Task<SpExecResult<sp_user_create__OutParameterValues>> sp_user_create__Async(CancellationToken cancellationToken,
                string pUsername, string pEmail, string pMobile, string pPasswordHash)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pUsername", pUsername),
                    CreateParameter("@pEmail", pEmail),
                    CreateParameter("@pMobile", pMobile),
                    CreateParameter("@pPasswordHash", pPasswordHash),
                    pResponseCodeParameter,
                };

                await this.StoredProcedures.ExecStoredProcedureAsync("[auth].[sp_user_create]", parameters, cancellationToken);
                var outParameterValues = new sp_user_create__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return ExecResult(outParameterValues);
            }

            public class sp_user_create__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }

            /// <summary>
			/// Stored procedure
			/// <code>[auth].[sp_user_login]</code>
			/// </summary>
			public async Task<SpSelectResult<UserEntity, sp_user_login__OutParameterValues>> sp_user_login__Async(CancellationToken cancellationToken, string pUsername, string pPasswordHash)
            {
                var pResponseCodeParameter = CreateOutputParameter("@pResponseCode", SqlDbType.Int);

                var parameters = new[]
                {
                    CreateParameter("@pUsername", pUsername),
                    CreateParameter("@pPasswordHash", pPasswordHash),
                    pResponseCodeParameter,
                };

                var dt = await this.StoredProcedures.SelectTableFromStoredProcedureAsync("[auth].[sp_user_login]", parameters, cancellationToken);
                var result = ToList<UserEntity>(dt).FirstOrDefault();
                var outParameterValues = new sp_user_login__OutParameterValues();
                outParameterValues.pResponseCode = ToValue<PPResponseCode>(pResponseCodeParameter) ?? default(PPResponseCode);
                return SelectResult(result, outParameterValues);
            }

            public class sp_user_login__OutParameterValues
            {
                public PPResponseCode pResponseCode;
            }
        }
    }
}
