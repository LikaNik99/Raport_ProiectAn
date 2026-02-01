using System.Data;

namespace PayBank.Models
{
    public partial class UserDetails
    {
        public int UserId { get; set; }
        public string Name { get; set; }
        public string Email { get; set; }
        public string Password { get; set; }
        public string Mobile { get; set; }
        public DateTime CreatedDate { get; set; }
        public ICollection<UserRole> UserRoles { get; set; }
    }
    public class Role
    {
        public int RoleId { get; set; }
        public string RoleName { get; set; }

        public ICollection<UserRole> UserRoles { get; set; }
    }
    public class UserRole
    {
        public int UserId { get; set; }
        public UserDetails Userdetails { get; set; }

        public int RoleId { get; set; }
        public Role Role { get; set; }
    }
}
