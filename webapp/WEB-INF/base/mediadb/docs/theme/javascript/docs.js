jQuery(document).ready(function() 
{ 
	$('.select-dropdown-open').on("click",function(){
		
		if ($(this).hasClass('down')) {
			$(this).removeClass('down');
			$(this).addClass('up');
			$(this).siblings('.select-dropdown').show();
		} else {
			$(this).removeClass('up');
			$(this).addClass('down');
			$(this).siblings('.select-dropdown').hide();
		}
	});
	$('.select-dropdown li a').on("click",function(){
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').removeClass('up');
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').addClass('down');
		$(this).closest('.select-dropdown').hide();
	});
});
