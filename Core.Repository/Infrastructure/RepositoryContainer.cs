using Core.Repositories.Infrastructure.Base;

namespace Core.Repositories.Infrastructure
{
    public class RepositoryContainer : RepositoryContainerBase
    {
        public Repositories.Repository.Book.AuthorRepository Author => this.GetRepository<Repositories.Repository.Book.AuthorRepository>();
        public Repositories.Repository.Book.CategoryRepository Category => this.GetRepository<Repositories.Repository.Book.CategoryRepository>();
        public Repositories.Repository.Book.BookRepository Book => this.GetRepository<Repositories.Repository.Book.BookRepository>();
        public Repositories.Repository.Book.ReviewRepository Review => this.GetRepository<Repositories.Repository.Book.ReviewRepository>();
        public Repositories.Repository.Auth.UserRepository User => this.GetRepository<Repositories.Repository.Auth.UserRepository>();
       

        public RepositoryContainer(string connectionString, bool isTransactioned = false) : base(connectionString, isTransactioned) { }
        public static RepositoryContainer Create() => new RepositoryContainer(ConnectionStrings.GetBaseDB());
        public static RepositoryContainer CreateTransactioned() => new RepositoryContainer(ConnectionStrings.GetBaseDB(), isTransactioned: true);
    }
}
