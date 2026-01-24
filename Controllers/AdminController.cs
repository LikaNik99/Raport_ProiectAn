using Microsoft.AspNetCore.Mvc;

namespace PasswordManager.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class AdminController : Controller
    {
        [HttpGet("{request}")]
        public async Task<IActionResult> Get(string request)
        {
            return View("Views/admin_request.cshtml");
        }
    }
}
