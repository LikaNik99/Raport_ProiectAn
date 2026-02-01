namespace Core.Repository.Config.DBModel
{
    public class BookEntity
    {
		public int Id { get; set; }
		public string Titlu { get; set; }
		public int AnPublicare { get; set; }
		public string ImageFileName { get; set; }
		public int AuthorId { get; set; }
		public string AutorName { get; set; }
		public int CategoryId { get; set; }
		public string CategoryName { get; set; }
		public string Recenzor { get; set; }
		public string Mesaj { get; set; }
    }
}
