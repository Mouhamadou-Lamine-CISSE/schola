Spine = require('spine')

UserTag = require('models/admin/UserTag')

class UserTag extends Spine.Model

  @configure 'UserTag', 'label', 'userId'

  @Reload: ->

    $.Deferred (deferred) =>

      d = $.Deferred()

      d.done (userTags) => 
        @refresh(userTags, clear: yes)
        deferred.resolve @all()  

      app.labels.getLabels()
         .done d.resolve

  @getUserTags: (userId) ->
    UserTag.select (userTag) => userTag.userId is userId
  
module.exports = UserTag