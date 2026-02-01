namespace PayBank.Models
{
    public class ReviewViewModel
    {
        public int Id { get; set; }
        public string Recenzor { get; set; }
        public string Mesaj { get; set; }
        public int BookId { get; set; }
    }
}
