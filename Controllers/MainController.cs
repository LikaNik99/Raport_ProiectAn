using HrWebRecruitment.Services;
using Microsoft.AspNetCore.Mvc;

namespace HrWebRecruitment.Controllers
{
    public class MainController(DbService dbService) : Controller
    {
        [HttpGet]
        public IActionResult Index()
        {
            return View();
        }

        [HttpGet("/Admin")]
        public IActionResult Admin()
        {
            return View();
        }
    }
}
