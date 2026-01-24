$(document).ready(function () {
    const DivContainer = $("#vacanciescont");
    var displayedText;

    $('.applyDone').hide();

    function getCookie(cname) {
        let name = cname + "=";
        let decodedCookie = decodeURIComponent(document.cookie);
        let ca = decodedCookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length, c.length);
            }
        }
        return "";
    }

    function validatePhoneNumber(phoneNumber) {
        // Expresia regulată pentru verificarea formatului numărului de telefon
        var regex = /^(0?\d{7})$/;

        // Verificăm dacă numărul de telefon se potrivește cu expresia regulată
        if (regex.test(phoneNumber)) {
            return true; // Numărul de telefon este valid
        } else {
            return false; // Numărul de telefon nu este valid
        }
    }

    $.ajax({
        method: "POST",
        url: "/vacancies/get",
        data:
        {
            action: "showVacancies"
        },
        dataType: 'json',
        success: function (data) {
            $(data).each(function (index, data) {
                newDiv = $('<div>').addClass('vacanciesbox')
                    .appendTo(DivContainer);
                $('<h3>')
                    .addClass('PositionTitl')
                    .html(data.Title)
                    .appendTo(newDiv);
                    
                $('<p>')
                    .appendTo(newDiv);
                    //$('<div>')
                    
                    $('<div>')
                    .addClass('PositionMiniDescription')
                    .html(data.Description)
                    .attr("style", "display: none;")
                   .appendTo(newDiv);

                $('<a>')
                    .addClass('vacanciesbtn')
                    .text('Afla mai multe')
                    .attr("value", data.EndDate)
                    .appendTo(newDiv);
            });

            // $(document).on('dblclick', function (e) {
            //     if (!(($(e.target).closest(".VacanciesContent").length > 0) || ($(e.target).closest(".VacantionPosition").length > 0) || ($(e.target).closest(".CV-PlacerBody").length > 0))) {
            //         $(".VacanciesModalDetails").hide();
            //         $('.VacanciesContent').css({
            //             'float': '',
            //             'margin-left': ''
            //         });
            //         $('.CV-PlacerBody').hide();
            //         $('.applyDone').hide();
            //         $("#contact").css("display", "");
            //     }
            // });
            $('.vacanciesbtn').on('mousedown', function () { // deschiderea meniului Vacantiei
                var title = $(this).parent().find('.PositionTitl').text();
                var desc = $(this).parent().find('.PositionMiniDescription').text();
                $('.VacanciesContent-Title').html('<h1>' + title + '</h1>');
                var date = $(this).attr("value");
                
                // var date = $(this).find('.learnMore').val();
                
                $('.VacanciesContent-EndTime').text('Deadline: ' + date);
                $('.VacanciesContent-Description').text(desc);
                
                $("#contact").css("display", "none");


                $('.VacanciesModalDetails').on('mousedown', function (e) {
                    if (!(($(e.target).closest(".VacanciesContent").length > 0) || ($(e.target).closest(".VacantionPosition").length > 0) || ($(e.target).closest(".CV-PlacerBody").length > 0))) {
                        $(".VacanciesModalDetails").hide();
                        $('.VacanciesContent').css({
                            'float': '',
                            'margin-left': ''
                        });
                        $('.CV-PlacerBody').hide();
                        $('.applyDone').hide();
                        $("#contact").css("display", "");
                    }
                });
                //$(".formSubmit").on('click', function () {
                //    alert("Message1");
                //});
                //CV-PlacerBody

                //$('#formInputPhone').on('input', function (event) {
                //    var isNumPad = event.location === KeyboardEvent.DOM_KEY_LOCATION_NUMPAD;
                //    var isNumpadKey = event.originalEvent && event.originalEvent.keyIdentifier && event.originalEvent.keyIdentifier.startsWith('U+');

                //    if (isNumPad && keyCode >= 96 && keyCode <= 105) {
                //        return;
                //    }
                //    event.preventDefault();
                //    if (validatePhoneNumber($(this).val())) {
                //        $(this).css('border', '2px solid #71A466');
                //    } else {
                //        $(this).css('border', '2px solid #A42822');
                //    }
                //});

                $('#formInputPhone').on('keydown', function (event) {
                    var key = event.key;
                    var keycode = event.keyCode;
                    if (validatePhoneNumber($(this).val())) {
                        $(this).css('border', '2px solid #71A466');
                    } else {
                        $(this).css('border', '2px solid #A42822');
                    }   
                    // Verificăm dacă a fost apăsată tasta de delete sau backspace sau cifrele de pe numpad
                    if (key === 'Delete' || key === 'Backspace') {
                        return; // Permitere ștergere sau introducere din numpad
                    }

                    // Verificăm dacă tasta apăsată este un număr sau '+'
                    if ((keycode >= 48 && keycode <= 57) || (keycode >= 96 && keycode <= 105)) {
                        return; // Permitere introducere cifre și '+'
                    }
                    event.preventDefault();
                    
                });

                $('.formSubmit').on('click', function (e) {
                    e.preventDefault();
                    var formData = new FormData();
                    formData.append("VacancyTitle", title);
                    formData.append("Name", $('.formInputName').val());
                    formData.append("Surname", $('.formInputSurname').val());
                    formData.append("Email", $('.formInputEmail').val());
                    formData.append("Phone", $('.formInputPhone').val());
                    formData.append("file", $('#UploadCV')[0].files[0]);
                    $.ajax({
                        method: "POST",
                        url: "/aplicate",
                        processData: false,
                        contentType: false,
                        cache: false,
                        data: formData,
                        enctype: 'multipart/form-data',
                        //{
                        //    action: "setCandidat",
                        //    VacancyTitle: title,
                        //    Name: $('.formInputName').val(),
                        //    Surname: $('.formInputSurname').val(),
                        //    Email: $('.formInputEmail').val(),
                        //    Phone: $('.formInputPhone').val(),
                        //    formData
                        //},
                        dataType: 'json'
                    }).always(function () {
                        $('.applyDone').show();
                        $('.formInput').val('');
                        $('#UploadCV').val('');
                    });
                });
                
                //$(this).find()
                //alert($(this).find('.PositionTitl').text());
                // is open
                //$('.CV-PlacerBody').hide();
                //$(document).closest('.VacanciesModalDetails').show();
                //$('.VacanciesModalDetails').css('display', 'block');

                $('.VacanciesModalDetails').show();
                $('#PlaceCV').on('click', function () {
                    $('.VacanciesContent').css({
                        'float': 'left',
                        'margin-left': '200px'
                    });
                    $('.CV-PlacerBody').show();
                });
               
                $('#UploadCV').on('change', function () {
                    $('.formLabel').text($('#UploadCV').val().split('\\').pop());
                })
                
                // is closed
            });
            
        }
        
    });
    

    //$('.learnMore').on('click', function () {
    //    alert($('.PositionTitle').text());
    //});

    
    $(document).on('mousedown', function (e) {
        if (!($(e.target).closest(".modal-content").length  || $(e.target).closest("#loginBtn").length)) {
            $("#myModal").hide();
        }
    });
    var role = getCookie("role");
    if (role != null && role != "") {
        $("#loginBtn").html("Personal account");
        $("#loginBtn").on('click', function () {
            window.location.href = '/admin';
        });
    } else {
        $("#loginBtn").click(function () {
            $("#myModal").show();
            $("#myModal").css('display', 'block');
        });
        $("#close").click(function () {
            $("#myModal").hide();
            $("#myModal").css('display', 'none');
        });
    $('#UserLogin').on('click', function (event) {
        event.preventDefault();

        if ($("#uname").val() === "" || $("#pname").val() === "") {
            alert("Introduceti loginul sau parola");
            return false;
        }
        else {
            $.ajax({
                method: "POST",
                url: "/user/login",
                data:
                {
                    action: "checkUser",
                    uname: $('#uname').val(),
                    pname: $('#pname').val()
                },
                dataType: 'json',
                success: function (data) {
                    console.log(data);
                    if (data == null) {
                        alert('incorect username or password');
                    } else {
                        if (data.RoleId === 1) {
                            document.cookie = 'role=Admin';
                            window.location.href = '/admin';
                                //$("#loginform").attr('action', 'AdminPanel.aspx');
                        } else {
                            document.cookie = "role=User";
                            window.location.href = '/admin';
                                //$("#loginform").attr('action', 'Login.aspx');
                            }
                        //$(data).each(function (index, dat) {
                        //})
                    }
                }
            });
        }
    });
    }
})