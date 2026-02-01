namespace Core.DBModel
{
    public class EnityOperation
    {
        public int Id { get; set; }
        public DateTime RegDate { get; set; }
        public long Amount { get; set; }
        public int Status { get; set; }
        public string UserName { get; set; } = null!;
        public string ServiceName { get; set; } = null!;
        public int ProviderId { get; set; }
    }
}
