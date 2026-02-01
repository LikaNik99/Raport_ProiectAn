using System.Security.Claims;
using Core.Model.Common;
using Core.Repositories;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using PayBank.Models;

namespace PayBank.Controllers
{
    public class AcountController : Controller
    {
        private static UsersReposiotry userRepository = new UsersReposiotry();


        public IActionResult Index()
        {
            return View();
        }
        public IActionResult Login()
        {
            return View();
        }
        [HttpPost]
        public async Task<ActionResult> Login(LoginViewModel model)
        {
            if (ModelState.IsValid)
            {
                var result = await userRepository.Login(model.Name, model.Password);


                if (result.ResultCode != PPResponseCode.SUCCESS)
                {
                    TempData["Message"] = "Email-ul sau parola au fost introduse gresit.";
                    TempData["AlertType"] = "danger";
                    return View("Login", model);
                }
                else
                {
                    

                    var claims = new List<Claim>
                    {
                    new Claim(ClaimTypes.Name, result.ReturnObject!.Username),
                    new Claim(ClaimTypes.Email, result.ReturnObject.Email),
                    new Claim("UserId", result.ReturnObject.Id.ToString())
                    };

                    claims.Add(new Claim(ClaimTypes.Role, result.ReturnObject.RoleName));
                    

                    var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
                    var authProperties = new AuthenticationProperties
                    {
                        IsPersistent = model.RememberMe
                    };

                    await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(claimsIdentity), authProperties);
                    return RedirectToAction("Services", "Home");
                }
            }

            return View("Login", model);
        }
        public IActionResult Registration()
        {
            ViewData["Message"] = "Registration Page";

            return View();
        }
        [HttpPost]
        public async Task<ActionResult> Registar(RegistrationViewModel model)
        {
            if (ModelState.IsValid)
            {
                try
                {
                    var result = await userRepository.AddUser(
                        model.Name,
                        model.Email,
                        model.Mobile,
                        model.Password
                        );

                    if (result.ResultCode == PPResponseCode.SUCCESS)
                    {
                        return RedirectToAction("Index", "Acount");
                    }
                    else if (result.ResultCode == PPResponseCode.USER_NAME_EXISTS)
                    {
                        TempData["Message"] = "User Name-ul introdus este deja folosit.";
                        TempData["AlertType"] = "danger";
                        return View("Registration", model);
                    }
                    else
                    {
                        TempData["Message"] = "A apărut o eroare la salvarea utilizatorului.Vă rugăm încercați din nou mai târziu.";
                        TempData["AlertType"] = "danger";
                        return View("Registration", model);
                    }
                }
                catch (Exception ex)
                {
                    TempData["Message"] = "A apărut o eroare la salvarea utilizatorului.Vă rugăm încercați din nou mai târziu.";
                    TempData["AlertType"] = "danger";
                    return View("Registration", model);
                }
            }
            else
            {
                TempData["Message"] = "Vă rugăm sa completati toate datele necesare corect!";
                TempData["AlertType"] = "danger";
                return View("Registration", model);
            }
        }

        public async Task<IActionResult> Logout()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            HttpContext.Session.Clear();
            return RedirectToAction("Services", "Home");
        }

        public IActionResult AccessDenied()
        {
            return View();
        }
    }
}

