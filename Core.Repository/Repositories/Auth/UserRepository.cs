using Core.Repositories.Infrastructure;
using Core.Repository.Config.DBModel;
using static Core.Repositories.Infrastructure.Base.StoredProceduresBase;
using static Core.Repositories.Infrastructure.StoredProcedures.auth__Schema;

namespace Core.Repositories.Repository.Auth
{
    public class UserRepository : RepositoryBase<Guid>
    {
        public async Task<SpExecResult<sp_user_create__OutParameterValues>> CreateUser__Async(string pUsername, string pEmail, string pMobile,
            string pPasswordHash, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.auth.sp_user_create__Async(cancellationToken, pUsername, pEmail, pMobile, pPasswordHash);

            return result;
        }

        public async Task<SpSelectResult<UserEntity, sp_user_login__OutParameterValues>> UserLogin__Async(string pUsername, string pPasswordHash, CancellationToken cancellationToken)
        {
            var result = await this.DbObjects.StoredProcedures.auth.sp_user_login__Async(cancellationToken, pUsername, pPasswordHash);

            return result;
        }
    }
}
