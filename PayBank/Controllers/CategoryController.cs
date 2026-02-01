using Core.Model.Common;
using Core.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PayBank.Models;

namespace PayBank.Controllers
{
    [Authorize(Policy = "AdminOnly")]
    public class CategoryController : Controller
    {
        private static CategoriesRepository categoryRepository = new CategoriesRepository();

        [HttpGet]
        public async Task<IActionResult> Index()
        {
            var categories = await categoryRepository.GetCategories();
            return View(categories.ReturnObject);
        }
        [HttpPost]
        public async Task<IActionResult> Index(string Search)
        {
            var result = await categoryRepository.GetCategories();
            if(result.ResultCode != PPResponseCode.SUCCESS)
            {
                return View();
            }
            var categories = result.ReturnObject;
            if (!String.IsNullOrEmpty(Search))
            {
                categories = categories!.Where(c => c.Name.ToLower().Contains(Search.ToLower()) || c.Id.ToString().Contains(Search)).ToList();
            }
            return View(categories);
        }
        public IActionResult Create()
        {
            return View();
        }

        [HttpPost]
        public async Task<IActionResult> Create(CreateCategoryViewModel model)
        {
            if (ModelState.IsValid)
            {
                var result = await categoryRepository.AddCategory(model.Name);

                if(result.ResultCode == PPResponseCode.SUCCESS)
                {
                    TempData["Message"] = "Categoria a fost adaugat cu succes";
                    TempData["AlertType"] = "success";
                }
                else
                {
                    TempData["Message"] = result.ResultCode.ToString();
                    TempData["AlertType"] = "danger";
                }
                
                return RedirectToAction("Create","Category");
            }
            return View(model);
        }
        [HttpGet]
        public async Task<IActionResult> Edit(int id)
        {
            var category = await categoryRepository.GetCategory(id);
            if (category.ResultCode != PPResponseCode.SUCCESS)
            {
                return RedirectToAction("Index", "Category");
            }
            var vm = new CreateCategoryViewModel
            {
                Name = category.ReturnObject!.Name,
            };
            ViewData["CategoryId"] = category.ReturnObject.Id;
            return View(vm);
        }
        [HttpPost]
        public async Task<IActionResult> Edit(int id, CreateCategoryViewModel vm)
        {
            if (ModelState.IsValid)
            {
                var result = await categoryRepository.UpdateCategory(id, vm.Name);
                return RedirectToAction("Index", "Category");
            }
            return View(vm);
        }
        [HttpDelete]
        public async Task<IActionResult> Delete(int id)
        {
            var result = await categoryRepository.DeleteCategory(id);
            if (result.ResultCode != PPResponseCode.SUCCESS)
            {
                return BadRequest(new { message = "Error deleting author", resultCode = result.ResultCode.ToString() });
            }
            return Ok();
        }

    }
}
