$(document).ready(function () {
    // --- Utility Functions & Initialization ---

    let currentItemID = ""; // Stores the ID of the item being edited/deleted
    let currentViewTarget = 'GetVacancy'; // Stores the current API target (e.g., 'GetVacancy')
    let CandidatID = 0;
    const role = getCookie("role");
    const StatusArray = [];

    // HTML Templates for Modals (Simplified, must match your backend form fields)
    const templates = {
        'vacancyForm': `
            <form id="vacancy-form">
                <div class="bodySelection">
                    <label for="SelectModel">Select a Model:</label>
                    <select id="SelectModel">
                        <option value="">Select Model...</option>
                    </select>
                </div>
                <div class="bodySelection">
                    <label for="VacTitle">Title</label>
                    <input type="text" id="VacTitle">
                </div>
                <div class="bodySelection">
                    <label for="SetDate">End Date</label>
                    <input type="date" id="SetDate">
                </div>
                <div class="bodySelection" style="grid-template-columns: 1fr;">
                    <label for="VacancyDesc">Description</label>
                    <textarea id="VacancyDesc" placeholder="Place Description"></textarea>
                </div>
                <div class="button-group">
                    <button type="button" id="EditVacancyButton" class="btn btn-primary" style="display: none;">Save Editing</button>
                    <button type="button" id="DeleteVacancyButton" class="btn btn-secondary" style="display: none;">Delete Vacancy</button>
                    <button type="button" id="AddVacancyButton" class="btn btn-success" style="display: none;">Add Vacancy</button>
                </div>
            </form>`,
        'dictionaryForm': `
        <form id="dictionary-edit-form">
            <div class="bodySelection"><label for="DictName">Name</label><input type="text" id="DictName"></div>
            <div class="bodySelection"><label for="DictType">Type</label><input type="text" id="DictType"></div>
            <div class="bodySelection" style="grid-template-columns: 1fr;">
                <label for="DictDescription">Description</label>
                <textarea id="DictDescription" rows="4"></textarea>
            </div>
            <div class="button-group">
                <button type="button" id="EditDictionaryButton" class="btn btn-primary" style="display: none;">Save Editing</button>
                <button type="button" id="DeleteDictionaryButton" class="btn btn-secondary" style="display: none;">Delete Item</button>
                <button type="button" id="AddDictionaryButton" class="btn btn-success" style="display: none;">Add Item</button>
            </div>
        </form>`,
        // Add more templates for Hiring, Users, Employees, Dictionary editing here
        // Example structure for a generic edit form:
        'editUserForm': `
            <form id="user-edit-form">
                <div class="bodySelection"><label for="UserFirstName">First Name</label><input type="text" id="UserFirstName"></div>
                <div class="bodySelection"><label for="UserLastName">Last Name</label><input type="text" id="UserLastName"></div>
                <div class="bodySelection"><label for="UserUsername">Username</label><input type="text" id="UserUsername"></div>
                <div class="bodySelection"><label for="UserPassword">Password</label><input type="text" id="UserPassword" placeholder="Leave blank to keep old password"></div>
                <div class="bodySelection"><label for="UserEmail">Email</label><input type="text" id="UserEmail"></div>
                <div class="button-group">
                    <button type="button" id="EditUserButton" class="btn btn-primary">Save Editing</button>
                    <button type="button" id="DeleteUserButton" class="btn btn-secondary">Delete User</button>
                </div>
            </form>`,
        'userForm': `
            <form id="user-edit-form">
                <div class="bodySelection"><label for="UserFirstName">First Name</label><input type="text" id="UserFirstName"></div>
                <div class="bodySelection"><label for="UserLastName">Last Name</label><input type="text" id="UserLastName"></div>
                <div class="bodySelection"><label for="UserUsername">Username</label><input type="text" id="UserUsername"></div>
                <div class="bodySelection"><label for="UserPassword">Password</label><input type="text" id="UserPassword" placeholder="Leave blank to keep old password"></div>
                <div class="bodySelection"><label for="UserEmail">Email</label><input type="text" id="UserEmail"></div>
                <div class="button-group">
                    <button type="button" id="AddUserButton" class="btn btn-success" style="display: none;">Add Item</button>
                </div>
            </form>`,
        'editHiringForm': `
        <form id="hiring-edit-form">
            <div class="bodySelection">
                <label for="HiringStatus">Current Status</label>
                <input type="text" id="HiringStatus" readonly>
                <button type="button" id="CandidatStatus" class="btn btn-secondary">Next Status</button>
            </div>
            <div class="bodySelection">
                <label for="HiringCommentary">Commentary</label>
                <textarea id="HiringCommentary" rows="4"></textarea>
            </div>
            <div class="button-group">
                <button type="button" id="EditHiringButton" class="btn btn-primary">Save Changes</button>
                <button type="button" id="DeleteHiringButton" class="btn btn-secondary">Remove Record</button>
            </div>
        </form>`,

        'editEmployeeForm': `
        <form id="employee-edit-form">
            <div class="bodySelection"><label for="EmpFirstName">First Name</label><input type="text" id="EmpFirstName"></div>
            <div class="bodySelection"><label for="EmpLastName">Last Name</label><input type="text" id="EmpLastName"></div>
            <div class="bodySelection"><label for="EmpPhone">Phone</label><input type="text" id="EmpPhone"></div>
            <div class="bodySelection"><label for="EmpEmail">Email</label><input type="text" id="EmpEmail"></div>
            <div class="bodySelection"><label for="EmpDepartment">Department</label><input type="text" id="EmpDepartment"></div>
            <div class="bodySelection"><label for="EmpPosition">Position</label><input type="text" id="EmpPosition"></div>
            <div class="bodySelection"><label for="EmpStartDate">Start Date</label><input type="date" id="EmpStartDate"></div>
            <div class="button-group">
                <button type="button" id="EditEmployeeButton" class="btn btn-primary">Save Editing</button>
                <button type="button" id="DeleteEmployeeButton" class="btn btn-secondary">Delete Employee</button>
            </div>
        </form>`,

        'editDictionaryForm': `
        <form id="dictionary-edit-form">
            <div class="bodySelection"><label for="DictName">Name</label><input type="text" id="DictName"></div>
            <div class="bodySelection"><label for="DictType">Type</label><input type="text" id="DictType"></div>
            <div class="bodySelection" style="grid-template-columns: 1fr;">
                <label for="DictDescription">Description</label>
                <textarea id="DictDescription" rows="4"></textarea>
            </div>
            <div class="button-group">
                <button type="button" id="EditDictionaryButton" class="btn btn-primary">Save Editing</button>
                <button type="button" id="DeleteDictionaryButton" class="btn btn-secondary">Delete Item</button>
            </div>
        </form>`
        // ... (other templates omitted for brevity, but follow this structure)
    };

    // Helper to read cookies
    function getCookie(cname) {
        let name = cname + "=";
        let decodedCookie = decodeURIComponent(document.cookie);
        let ca = decodedCookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) == ' ') c = c.substring(1);
            if (c.indexOf(name) == 0) return c.substring(name.length, c.length);
        }
        return "";
    }

    // Helper to handle AJAX requests and re-render the current view
    function performActionAndRefresh(url, data, targetRefresh) {
        $.post(url, data)
            .done(function (res) {
                // If the action was successful, re-fetch the data for the current view
                $.getJSON("/Admin/" + targetRefresh, { action: "GetComponents", target: targetRefresh }, function (data) {
                    switch (targetRefresh) {
                        case 'GetVacancy': GetVacancy(data); break;
                        case 'GetCandidats': GetCandidats(data); break;
                        case 'GetHiring': GetHiring(data); break;
                        case 'GetUsers': GetUsers(data); break;
                        case 'GetEmployees': GetEmployees(data); break;
                        case 'GetDictionary': GetDictionary(data); break;
                    }
                    closeModal();
                });
            })
            .fail(function (xhr) {
                console.error("Action failed! Status: " + xhr.status + "\nMessage: " + (xhr.responseJSON?.message || xhr.responseText));
            });
    }

    // Modal Helpers
    function openModal(title, contentHtml) {
        $('#modal-title').text(title);
        $('#modal-content-area').html(contentHtml);
        $('#global-modal-behind').fadeIn(200);
    }

    function closeModal() {
        $('#global-modal-behind').fadeOut(200);
        currentItemID = ""; // Reset ID on close
    }

    // Initial UI setup
    // Logout
    $('#LogOutModal-btn').on('click', function () {
        document.cookie = "role=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT"; // Clear cookie
        document.location.href = "/";
    });

    // Close Modal Button
    $('.close-modal-btn').on('click', closeModal);

    // Close modal when clicking outside (the background)
    $('#global-modal-behind').on('click', function (e) {
        if (e.target.id === 'global-modal-behind') {
            closeModal();
        }
    });

    // Fetch Statuses (Initial load)
    $.getJSON("/Admin/GetStatuses", { action: "GetComponents" }, function (data) {
        $.each(data, function (_, num) { StatusArray.push(num); });
    });

    function GetNextStatus(currentStatus) {
        var index = StatusArray.indexOf(currentStatus);
        return (index >= 0 && index < StatusArray.length - 1) ? StatusArray[index + 1] : StatusArray[0] || 'New';
    }

    // --- Table Renderers ---

    function renderTable(selector, headHtml, rowHtmlCallback, data) {
        let i = 1;
        const HeadTable = $(selector + ' thead');
        const BodyTable = $(selector + ' tbody');
        HeadTable.empty().append(headHtml);
        BodyTable.empty();
        $.each(data, function (_, num) {
            BodyTable.append(rowHtmlCallback(num, i++));
        });
    }

    function GetVacancy(data) {
        renderTable('#VacanciesTable',
            '<tr><th width="10px">№</th><th width="80px">Title</th><th width="300px">Description</th><th width="50px">StartDate</th><th width="50px">EndDate</th><th width="20px">Action</th></tr>',
            function (num, i) {
                // Use data-id for easier jQuery targeting
                return `<tr class="editable-row" data-id="${num.Id}" data-target="GetVacancy">
                            <td align="center">${i}</td>
                            <td class="Title">${num.Title}</td>
                            <td class="Description">${num.Description}</td>
                            <td class="StartDate">${formatISODate(num.StartDate)}</td>
                            <td class="EndDate">${formatISODate(num.EndDate)}</td>
                            <td class="Action"><a href="#" class="edit-btn"><i class="fas fa-edit"></i> edit</a></td></tr>`;
            }, data);

        // Populate SelectModel for Vacancy Add/Edit modal
        $('#SelectModel').empty().append('<option selected value="">Select Model...</option>');
        $.each(data, function (_, num) { $('#SelectModel').append('<option>' + num.Title + '</option>'); });
        $('#SelectModel').append('<option>Custom Model</option>');
    }

    function GetHiring(data) {
        renderTable('#HiringTable',
            '<tr><th width="10px">№</th><th width="80px">Candidat</th><th width="80px">User</th><th width="80px">Status</th><th width="80px">Vacancy</th><th width="10px">StatusDate</th><th width="180px">Commentary</th><th width="20px">Action</th></tr>',
            function (num, i) {
                const editLink = role === "Admin" ? ' </td><td class="Action"><a href="#" class="edit-btn"><i class="fas fa-edit"></i> edit</a>' : '';

                return `<tr class="editable-row" data-id="${num.HiringId}" data-candidat-id="${num.CandidatId}" data-target="GetHiring">
                            <td align="center">${i}</td>
                            <td class="Candidat">${num.Candidat}</td>
                            <td class="Users">${num.User}</td>
                            <td class="Status" data-cv="${num.CV}">${num.Status}</td>
                            <td class="Vacancy">${num.Vacancy}</td>
                            <td class="StatusDate">${formatISODate(num.StatusDate)}</td>
                            <td class="Commentary">${num.Comm}${editLink}</td></tr>`;
            }, data);
    }

    // NOTE: GetUsers, GetEmployees, GetDictionary renderers should also be updated
    // to use the new HTML structure with data-target and data-id on the <tr>.
    // Function to render the USERS table
    function GetUsers(data) {

        renderTable('#UsersTable',
            '<tr><th width="10px">№</th><th width="80px">Username</th><th width="80px">Password</th><th width="80px">FirstName</th><th width="80px">LastName</th><th width="80px">E-mail</th><th width="50px">RoleId</th><th width="80px">StartDate</th><th width="80px">EndDate</th><th width="20px">Action</th></tr>',
            function (num, i) {
                // Determine if the edit link should be shown (based on role)
                const editLink = role === "Admin" ? ' </td><td class="Action"><a href="#" class="edit-btn"><i class="fas fa-edit"></i> edit</a>' : '';

                return `<tr class="editable-row" data-id="${num.Id}" data-target="GetUsers">
                            <td align="center">${i}</td>
                            <td class="Username">${num.Username}</td>
                            <td class="Password">${num.Password}</td>
                            <td class="FirstName">${num.FirstName}</td>
                            <td class="LastName">${num.LastName}</td>
                            <td class="Email">${num.Email}</td>
                            <td class="RoleID">${num.RoleId}</td>
                            <td class="StartDate">${formatISODate(num.StartDate)}</td>
                            <td class="EndDate">${num.EndDate}${editLink}</td></tr>`;
            }, data);
    }

    // Function to render the EMPLOYEES table
    function GetEmployees(data) {
        renderTable('#EmployeesTable',
            '<tr><th width="10px">№</th><th width="80px">FirstName</th><th width="80px">LastName</th><th width="80px">Phone</th><th width="100px">E-mail</th><th width="50px">Department</th><th width="50px">Position</th><th width="20px">Hiring</th><th width="80px">StartDate</th><th width="20px">Action</th></tr>',
            function (num, i) {
                const editLink = role === "Admin" ? ' </td><td class="Action"><a href="#" class="edit-btn"><i class="fas fa-edit"></i> edit</a>' : '';
                return `<tr class="editable-row" data-id="${num.Id}" data-target="GetEmployees">
                            <td align="center">${i}</td>
                            <td class="FirstName">${num.FirstName}</td>
                            <td class="LastName">${num.LastName}</td>
                            <td class="Phone">${num.Phone}</td>
                            <td class="Email">${num.Email}</td>
                            <td class="Department">${num.Department}</td>
                            <td class="Position">${num.Position}</td>
                            <td class="Hiring">${num.Hiring}</td>
                            <td class="StartDate">${num.StartDate}${editLink}</td></tr>`;
            }, data);
    }

    // Function to render the CANDIDATES table
    function GetCandidats(candidateData) {

        // Check if employee data is already globally available (optional cache)
        // If not, we must fetch it.

        // 1. Fetch Employee Data (HRUserList)
        $.ajax({
            method: "GET",
            url: "/Admin/GetEmployees", // <<< CONFIRM YOUR ACTUAL EMPLOYEE FETCH ENDPOINT
            data: { action: "GetRecruiters" }, // Assuming an action to filter for recruiters/assignees
            dataType: "json"
        })
            .done(function (HRUserList) {
                // --- Employee data is now locally available as HRUserList ---
                const currentRole = getCookie("role");

                // --- 2. Function to create the User Assignment Selector ---
                function createAssignmentSelector(candidateId, currentlyAssignedUserId) {
                    // Helper function to get the full name from a user object
                    const getFullName = (user) => `${user.FirstName} ${user.LastName}`;

                    // Only Admins or roles with assignment permission should see the selector
                    if (currentRole !== "Admin" && currentRole !== "Manager") {
                        // Find the assigned user object
                        const assignedUser = (HRUserList && HRUserList.length > 0)
                            ? HRUserList.find(user => user.Id === currentlyAssignedUserId)
                            : null;

                        // Display the full name or 'N/A'
                        const assignedUserName = assignedUser ? getFullName(assignedUser) : 'N/A';

                        return `<span class="assigned-user-name">${assignedUserName}</span>`;
                    }

                    let selectorHtml = `<select class="assign-user-select" data-candidate-id="${candidateId}">`;

                    // Add a default/unassigned option
                    const isUnassignedSelected = !currentlyAssignedUserId ? 'selected' : '';
                    selectorHtml += `<option value="" ${isUnassignedSelected}>-- Assign Recruiter --</option>`;

                    // Populate options from the fetched HRUserList
                    if (HRUserList && HRUserList.length > 0) {
                        HRUserList.forEach(user => {
                            // Check if this user is the one currently assigned
                            const isSelected = (user.Id === currentlyAssignedUserId) ? 'selected' : '';

                            // **CHANGE HERE: Use full name for the displayed text**
                            const fullName = getFullName(user);

                            // Use user.id for the value and the full name for the visible text
                            selectorHtml += `<option value="${user.Id}" ${isSelected}>${fullName}</option>`;
                        });
                    } else {
                        selectorHtml += '<option value="" disabled>No HR Users Found</option>';
                    }
                    selectorHtml += '</select>';
                    return selectorHtml;
                }
                // -----------------------------------------------------------

                // 3. Render the Candidate Table
                renderTable('#CandidatsTable',
                    '<tr><th width="10px">№</th><th width="80px">FirstName</th><th width="80px">LastName</th><th width="80px">Email</th><th width="80px">Phone</th><th width="80px">Link to CV</th><th width="80px">Source</th><th width="120px">Assign Recruiter</th></tr>',
                    function (num, i) { // num = individual candidate object

                        // Extract the currently assigned user ID from the candidate data
                        const assignedUserId = num.AssignedUserId;
                        // Get the assignment selector HTML using the fetched HRUserList
                        const assignmentSelector = createAssignmentSelector(num._id, assignedUserId);
                        // Ensure the CV link is clickable
                        const cvLink = num.LinkToCv ? `<a href="${num.LinkToCv}" target="_blank" class="cv-link"><i class="fas fa-file-alt"></i> View CV</a>` : 'N/A';

                        // Add a status icon based on assignment status
                        const statusIcon = assignedUserId ?
                            '<i class="fas fa-user-check assigned" title="Assigned"></i>' :
                            '<i class="fas fa-user-times unassigned" title="Unassigned"></i>';

                        return `<tr class="editable-row" data-id="${num._id}" data-target="GetCandidats">
                            <td align="center">${i}</td>
                            <td class="FirstName">${num.FirstName}</td>
                            <td class="LastName">${num.LastName}</td>
                            <td class="Email">${num.Email}</td>
                            <td class="Phone">${num.PhoneNumber}</td>
                            <td class="LinkToCv">${cvLink}</td>
                            <td class="Source">${num.Source || 'N/A'}</td>
                            
                            <td class="Action">
                                ${statusIcon}
                                ${assignmentSelector}
                            </td>
                        </tr>`;
                    }, candidateData); // Use the original candidateData received
            })
            .fail(function (xhr) {
                console.error("Failed to fetch employee list:", xhr);
                // Optionally render the candidate table without the selector if data is critical
                renderTable('#CandidatsTable',
                    '<tr><th>Error</th></tr>',
                    () => `<tr><td colspan="8">Could not load employee list for assignment. Please check the backend connection.</td></tr>`,
                    [{}] // Render one row to show the error
                );
            });
    }

    // Function to render the DICTIONARY table
    function GetDictionary(data) {
        renderTable('#DictionaryTable',
            '<tr><th width="10px">№</th><th width="80px">Name</th><th width="80px">Type</th><th width="200px">Description</th></tr>',
            function (num, i) {
                const editLink = role === "Admin" ? ' <a href="#" class="edit-btn"><i class="fas fa-edit"></i> edit</a>' : '';

                return `<tr class="editable-row" data-id="${num._id}" data-target="GetDictionary">
                            <td align="center">${i}</td>
                            <td class="Name">${num.Name}</td>
                            <td class="Type">${num.Type}</td>
                            <td class="Description">${num.Description}${editLink}</td></tr>`;
            }, data);
    }

    // Add this handler to your HRupdate.js file
    $('#content-view').on('change', '.assign-user-select', function () {
        const assignedUserId = $(this).val();
        const candidateId = $(this).data('candidate-id');
        if (assignedUserId) {
            // 1. Confirm the action (optional)
            //if (!confirm(`Assign this candidate to user ID: ${candidateId}?`)) {
            //    // User cancelled, maybe reset the dropdown selection if possible
            //    return;
            //}

            // 2. Perform the AJAX call to assign the user (Requires C# API endpoint)
            $.post("/Admin/GetCandidates/AssignRecruiter", {
                CandidateID: candidateId,
                RecruiterID: assignedUserId
            }, function (response) {
                // Handle success response (e.g., show notification, or don't refresh)
                console.log('Assignment successful:', response);
                // Optional: Show a brief success message next to the selector
            }).fail(function (xhr) {
                // Handle error
                alert("Error assigning recruiter. Please check server logs.");
            });
        }
    });
    // --- Navigation & Data Loading (Revised to use new HTML structure) ---

    $('.nav-links').on('click', '.nav-item', function (e) {
        e.preventDefault();

        // Handle UI state (active class is handled in the inline JS block, but re-run for safety)
        $('.nav-item').removeClass('active');
        $(this).addClass('active');

        // Hide/Show tables (Views)
        const targetContent = $(this).data('content');
        const targetView = targetContent + '-view';
        $('.data-view').removeClass('active').addClass('hidden');
        $('#' + targetView).removeClass('hidden').addClass('active');

        // Determine API target
        const apiTargets = {
            'vacancies': 'GetVacancy',
            'candidats': 'GetCandidats',
            'hiring': 'GetHiring',
            'employees': 'GetEmployees',
            'dictionary': 'GetDictionary',
            'users': 'GetUsers'
        };

        currentViewTarget = apiTargets[targetContent];

        // Toggle add buttons
        $('.addUser').hide();
        $('.addVacancies').hide();
        $('.addDictionaries').hide();

        if (currentViewTarget === 'GetVacancy') {
            $('.addVacancies').show();
        } else if (currentViewTarget === 'GetDictionary') {
            $('.addDictionary').show();
        } else if (currentViewTarget === 'GetUsers' && role === "Admin") {
            $('.addUser').show();
        }

        // Fetch Data
        $.getJSON("/Admin/" + currentViewTarget, { action: "GetComponents", target: currentViewTarget }, function (data) {
            switch (currentViewTarget) {
                case 'GetVacancy': GetVacancy(data); break;
                case 'GetCandidats': GetCandidats(data); break;
                case 'GetHiring': GetHiring(data); break;
                case 'GetUsers': GetUsers(data); break;
                case 'GetEmployees': GetEmployees(data); break;
                case 'GetDictionary': GetDictionary(data); break;
            }
        });
    });

    // --- CRUD Actions (Centralized and Adapted) ---

    // 1. ADD Button Handler (Global Add New / Contextual Add Vacancy/User)
    $('.addVacancies, .addUser, .addDictionary, #add-item-btn').on('click', function () {
        let title = "Add New Item";
        let content = "";
        let actionTarget = "";

        // Determine the context based on the visible view
        const activeViewId = $('#content-view').find('.data-view.active').attr('id');

        if (activeViewId === 'vacancies-view') {
            title = "Add New Vacancy";
            content = templates.vacancyForm;
            actionTarget = 'SetNewVacancy';
        } else if (activeViewId === 'dictionary-view') {
            title = "Add New Dictionary";
            content = templates.dictionaryForm;
            actionTarget = 'SetNewDictionary';
        } else if (activeViewId === 'users-view') {
            title = "Add New User";
            content = templates.userForm; // Re-use the user form for add
            actionTarget = 'SetNewUser';
        } else {
            alert('Select a category (Vacancies/Users) to use the Add button.');
            return;
        }

        openModal(title, content);

        // Hide Edit/Delete, show Add for the specific form
        $('#EditVacancyButton, #DeleteVacancyButton, #EditUserButton, #DeleteUserButton').hide();
        $('#AddVacancyButton, #AddUserButton, #AddDictionaryButton').show();
    });

    // 2. EDIT Button Handler (Delegated event for all tables)
    $('#content-view').on('click', '.editable-row .edit-btn', function (e) {
        e.preventDefault();

        const $row = $(this).closest('tr');
        currentItemID = $row.data('id');
        currentViewTarget = $row.data('target'); // e.g., 'GetVacancy'

        let title = "Edit Item";
        let content = "";

        // Show Edit/Delete, hide Add for the specific form
        // NOTE: We only show the buttons relevant to the modal type below, 
        // so we can remove this line for better control:
        // $('#EditVacancyButton, #DeleteVacancyButton, #EditUserButton, #DeleteUserButton').show();
        // $('#AddVacancyButton, #AddUserButton').hide();

        // --- Start: Handle different item types ---
        switch (currentViewTarget) {
            case 'GetVacancy':
                title = "Edit Vacancy";
                content = templates.vacancyForm;
                openModal(title, content);
                $('#EditVacancyButton, #DeleteVacancyButton').show();
                // Fetch data and populate form fields
                // Assuming your backend responds with a JSON object or array of one object
                $.post("/Admin/GetVacancy/EditVacancyMenu", { ID: currentItemID }, function (data) {
                    const d = Array.isArray(data) ? data[0] : (typeof data === 'string' ? JSON.parse(data) : data);

                    $('#VacTitle').val(d.VacancyTitle || d.Title);
                    $('#VacancyDesc').val(d.VacancyDescription || d.Description);
                    // Use formatISODate utility for date parsing
                    $('#SetDate').val(formatISODate(d.EndDate).substring(0, 10));
                    $('#SelectModel').val('');
                });
                break;

            case 'GetHiring':
                title = "Edit Hiring Candidate";
                content = templates.editHiringForm;
                openModal(title, content);

                // Fetch Hiring Data and populate form fields
                $.post("/Admin/GetHiring/EditHiringMenu", { ID: currentItemID }, function (data) {
                    const d = Array.isArray(data) ? data[0] : (typeof data === 'string' ? JSON.parse(data) : data);

                    // Populate fields
                    $('#HiringStatus').val(d.Status);
                    $('#CandidatStatus').text("Next: " + GetNextStatus(d.Status));
                    $('#HiringCommentary').val(d.Comm);
                    // Store CandidatID if needed for further actions
                    CandidatID = d.CandidatID;
                });
                break;

            case 'GetUsers':
                title = "Edit User";
                content = templates.editUserForm;
                openModal(title, content);

                // Fetch User Data and populate form fields
                $.post("/Admin/GetUsers/EditUserMenu", { ID: currentItemID }, function (data) {
                    const d = Array.isArray(data) ? data[0] : (typeof data === 'string' ? JSON.parse(data) : data);
                    // Populate fields
                    $('#UserFirstName').val(d.FirstName);
                    $('#UserLastName').val(d.LastName);
                    $('#UserUsername').val(d.UserName);
                    // Password field is usually left blank for security reasons on edit
                    $('#UserPassword').val('');
                    $('#UserEmail').val(d.Email);
                });
                break;

            case 'GetEmployees':
                title = "Edit Employee Record";
                content = templates.editEmployeeForm;
                openModal(title, content);

                // Fetch Employee Data and populate form fields
                $.post("/Admin/GetEmployees/EditEmployeeMenu", { ID: currentItemID }, function (data) {
                    const d = Array.isArray(data) ? data[0] : (typeof data === 'string' ? JSON.parse(data) : data);
                    // Populate fields
                    $('#EmpFirstName').val(d.FirstName);
                    $('#EmpLastName').val(d.LastName);
                    $('#EmpPhone').val(d.Phone);
                    $('#EmpEmail').val(d.Email);
                    $('#EmpDepartment').val(d.Department);
                    $('#EmpPosition').val(d.Position);
                    $('#EmpStartDate').val(formatISODate(d.StartDate).substring(0, 10));
                });
                break;

            case 'GetDictionary':
                title = "Edit Dictionary Item";
                content = templates.editDictionaryForm;
                openModal(title, content);
                // Fetch Dictionary Data and populate form fields
                $.post("/Admin/GetDictionary/EditDictionaryMenu", { ID: currentItemID }, function (data) {
                    const d = Array.isArray(data) ? data[0] : (typeof data === 'string' ? JSON.parse(data) : data);

                    // Populate fields
                    $('#DictName').val(d.Name);
                    $('#DictType').val(d.Type);
                    $('#DictDescription').val(d.Description);
                });
                break;
        }
        // --- End: Handle different item types ---

        // NOTE: The individual Save/Delete buttons for each form (e.g., #EditHiringButton)
        // must also have their own click handlers attached to #global-modal-behind 
        // to complete the CRUD cycle.
    });

    // 3. CRUD LOGIC HANDLERS (Delegated events for buttons inside the modal)

    // A. Vacancy CRUD Handlers (Attach to the Modal container)
    $('#global-modal-behind').on('click', '#EditVacancyButton', function () {
        performActionAndRefresh("/Admin/GetVacancy/EditVacancy", {
            ID: currentItemID,
            Title: $('#VacTitle').val(),
            Description: $('#VacancyDesc').val(),
            EndDate: $('#SetDate').val()
        }, 'GetVacancy');
    });

    $('#global-modal-behind').on('click', '#DeleteVacancyButton', function () {
        if (confirm("Are you sure you want to delete this vacancy?")) {
            performActionAndRefresh("/Admin/GetVacancy/DeleteVacancy", { ID: currentItemID }, 'GetVacancy');
        }
    });

    $('#global-modal-behind').on('click', '#AddVacancyButton', function () {
        performActionAndRefresh("/Admin/GetVacancy/SetNewVacancy", {
            Title: $('#VacTitle').val(),
            Description: $('#VacancyDesc').val(),
            EndDate: $('#SetDate').val()
        }, 'GetVacancy');
    });

    // B. Vacancy Model Selection
    $('#global-modal-behind').on('change', '#SelectModel', function () {
        var val = $(this).val();
        if (val === "Custom Model" || val === "Select Model...") {
            $('#VacTitle, #VacancyDesc').val('');
        } else {
            $.post("/Admin/GetVacancy/getVacancyV", { Title: val }, function (data) {
                $.each(data, function (_, d) {
                    $('#VacTitle').val(d.Title);
                    $('#VacancyDesc').val(d.Description);
                    // Optionally set the end date if the model has one
                });
            });
        }
    });

    // C. User CRUD Handlers (Example, needs corresponding API endpoints)
    $('#global-modal-behind').on('click', '#EditUserButton', function () {
        performActionAndRefresh("/Admin/GetUsers/EditUser", {
            ID: currentItemID,
            FirstName: $('#UserFirstName').val(),
            LastName: $('#UserLastName').val(),
            Username: $('#UserUsername').val(),
            Password: $('#UserPassword').val(), // Careful with security!
            Email: $('#UserEmail').val()
        }, 'GetUsers');
    });

    // D. Hiring Status (Adapt the old logic to the new structure)
    // NOTE: This logic needs to be attached to the 'Edit Hiring' modal content
    $('#global-modal-behind').on('click', '#CandidatStatus', function () {
        var status = GetNextStatus($(this).val());
        $(this).val(status).text(status);
        var colors = { "New": "blue", "Archieved": "yellow", "Rejected": "red", "Hired": "green" };
        $(this).css('background-color', colors[status] || 'lightgreen');
    });
    // D. Edit Dictionary Item
    $('#global-modal-behind').on('click', '#EditDictionaryButton', function () {
        // 1. Gather data from the dictionary edit form fields
        const dictionaryData = {
            ID: currentItemID,
            Name: $('#DictName').val(),
            Type: $('#DictType').val(),
            Description: $('#DictDescription').val()
        };

        // 2. Perform the update action and refresh the 'GetDictionary' view
        performActionAndRefresh("/Admin/GetDictionary/EditDictionary", dictionaryData, 'GetDictionary');
    });

    // E. Delete Dictionary Item
    $('#global-modal-behind').on('click', '#DeleteDictionaryButton', function () {
        if (confirm("WARNING: Are you sure you want to delete this dictionary item? This could affect other records!")) {
            // 1. Send the ID for deletion
            const deleteData = {
                ID: currentItemID
            };

            // 2. Perform the delete action and refresh the 'GetDictionary' view
            performActionAndRefresh("/Admin/GetDictionary/DeleteDictionary", deleteData, 'GetDictionary');
        }
    });

    // F. Add New Dictionary Item
    // NOTE: This assumes you have logic in the main 'Add New' handler 
    // to show the dictionary form and the #AddDictionaryButton.
    $('#global-modal-behind').on('click', '#AddDictionaryButton', function () {
        // 1. Gather data from the dictionary edit form fields
        const dictionaryData = {
            Name: $('#DictName').val(),
            Type: $('#DictType').val(),
            Description: $('#DictDescription').val()
        };

        // 2. Perform the add action and refresh the 'GetDictionary' view
        performActionAndRefresh("/Admin/GetDictionary/SetNewDictionary", dictionaryData, 'GetDictionary');
    });

    // Block to handle adding a new User when the designated button is clicked
    $('#global-modal-behind').on('click', '#AddUserButton', function () {
        // 1. Gather data from the user creation form fields
        const userData = {
            // *** IMPORTANT: Adjust these IDs to match the fields in your actual "Add User" form/modal ***
            FirstName: $('#UserFirstName').val(), // Example User ID field
            LastName: $('#UserLastName').val(), // Example Password field
            Username: $('#UserUsername').val(), // Example Email field
            Password: $('#UserPassword').val(), // Example Password field
            Email: $('#UserEmail').val()  // Example Role selection field (should send the role ID)
            // Add any other necessary fields (e.g., Name, Surname, Phone)
        };

        // 2. Perform the add action and refresh the 'GetUsers' view (or equivalent)
        // You need to confirm the correct backend URL and the refresh target ID.
        performActionAndRefresh("/Admin/GetUsers/SetNewUser", userData, 'GetUsers' );
    });

    // --- Initial Load ---
    $.getJSON("/Admin/GetVacancy", { action: "GetComponents" }, GetVacancy);
    function formatISODate(isoDate, formatType = 'local') {
        let dateString;

        // Handle MongoDB BSON date structure (e.g., { $date: "2023-02-01T00:00:00Z" })
        if (typeof isoDate === 'object' && isoDate !== null && isoDate.hasOwnProperty('$date')) {
            dateString = isoDate.$date;
        } else if (typeof isoDate === 'string') {
            dateString = isoDate;
        } else {
            return 'N/A'; // Or handle error appropriately
        }

        // Attempt to create a Date object
        const date = new Date(dateString);

        // Check if the date is valid
        if (isNaN(date.getTime())) {
            return 'Invalid Date';
        }

        if (formatType === 'utc') {
            // Example: 2023-02-01 00:00 UTC
            return date.getUTCFullYear() + '-' +
                String(date.getUTCMonth() + 1).padStart(2, '0') + '-' +
                String(date.getUTCDate()).padStart(2, '0') +
                ' UTC';
        } else {
            // Formats the date based on the user's locale (e.g., 01/02/2023, 02:00:00)
            return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        }
    }
});