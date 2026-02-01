using System.ComponentModel.DataAnnotations.Schema;
using System.ComponentModel.DataAnnotations;

namespace PayBank.Models
{
    public class RegistrationViewModel
    {
        [Required]
        [StringLength(15, MinimumLength = 3)]
        //[StringLength(15, ErrorMessage = "Name length can't be more than 15.")]
        public string Name { get; set; }
        [Required]
        [RegularExpression(@"^(\d{9})$", ErrorMessage = "Numarul de telefon nu este valid")]
        public string Mobile { get; set; }
        [Required]
        [EmailAddress]
        public string Email { get; set; }

        [Required]
        [StringLength(20, MinimumLength = 6)]
        public string Password { get; set; }
        [Required]
        [NotMapped] 
        [Compare("Password")]
        public string ConfirmPassword { get; set; }

    }
}
