$table = $("#dataGrid");
$(function () {
    $(".table").treegrid({
        expanderExpandedClass: 'glyphicon glyphicon-minus',
        expanderCollapsedClass: 'glyphicon glyphicon-plus'
    });
    //更新排序
    $("input[name=listorder]").on('blur',function () {
        var _id = $(this).data('id')
        var _listorder = $(this).val();
        if (_listorder.length > 0 && !isNaN(_listorder)) {
            $.post( '/console/menu/listorder',{
                id: _id,
                listorder: _listorder
            },function (ret) {
                if(ret.status==1){
                    window.location.reload();
                }
            });
        }
    });
    //删除处理
    $(".remove").on('click', function () {
        var _this = $(this);
        layer.confirm('确定删除吗?', function(){
            $.getJSON('/console/menu/delete', {ids:_this.data('id')}, function(ret){
                if (ret.status){
                    layer.msg(ret.msg, {icon: 1},function () {
                        $('#'+_this.data('id')).hide();
                    });
                } else {
                    layer.msg(ret.msg, {icon: 2});
                }
            });
        });
    })
});

function getState(value) {
    if(value == "menu"){
        return "仅菜单";
    }else if(value == "auth"){
        return "菜单权限"
    }else{
        return "按钮权限"
    }
}