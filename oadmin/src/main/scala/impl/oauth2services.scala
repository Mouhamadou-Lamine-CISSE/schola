package schola
package oadmin

package impl

import org.clapper.avsl.Logger
import schola.oadmin.impl.CacheActor.{FindValue, PurgeValue}

trait OAuthServicesComponentImpl extends OAuthServicesComponent {
  self: OAuthServicesRepoComponent =>

  val oauthService = new OAuthServicesImpl

  class OAuthServicesImpl extends OAuthServices {

    def getUsers = oauthServiceRepo.getUsers

    def getUser(id: String) = oauthServiceRepo.getUser(id)

    def removeUser(id: String) = oauthServiceRepo.removeUser(id)

    def getPurgedUsers = oauthServiceRepo.getPurgedUsers

    def purgeUsers(users: Set[String]) = oauthServiceRepo.purgeUsers(users)

    def getToken(bearerToken: String) = oauthServiceRepo.getToken(bearerToken)

    def getTokenSecret(accessToken: String) = oauthServiceRepo.getTokenSecret(accessToken)

    def getRefreshToken(refreshToken: String) = oauthServiceRepo.getRefreshToken(refreshToken)

    def exchangeRefreshToken(refreshToken: String) = oauthServiceRepo.exchangeRefreshToken(refreshToken)

    def getUserTokens(userId: String) = oauthServiceRepo.getUserTokens(userId)

    def getUserSession(params: Map[String, String]) = oauthServiceRepo.getUserSession(params)

    def revokeToken(accessToken: String) = oauthServiceRepo.revokeToken(accessToken)

    def getClient(id: String, secret: String) = oauthServiceRepo.getClient(id, secret)

    def authUser(username: String, password: String) = oauthServiceRepo.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      oauthServiceRepo.saveToken(accessToken, refreshToken, macKey, uA, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, scopes)

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) = oauthServiceRepo.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts, passwordValid)

    def updateUser(id: String, spec: utils.UserSpec) = oauthServiceRepo.updateUser(id, spec)

    def getAvatar(id: String) = oauthServiceRepo.getAvatar(id)

    def emailExists(email: String) = oauthServiceRepo.emailExists(email)
  }
}

trait OAuthServicesRepoComponentImpl extends OAuthServicesRepoComponent {
  self: OAuthServicesComponent with AccessControlServicesComponent =>

  import Q._

  val log = Logger("oadmin.oauthserviceRepoImpl")

  protected val db: Database

  protected val oauthServiceRepo = new OAuthServicesRepoImpl

  class OAuthServicesRepoImpl extends OAuthServicesRepo {
    import oauthService._

    import schema._
    import domain._

    def getUsers = {
      import Database.dynamicSession

      val q = for {
        u <- Users if ! u._deleted
      } yield (
          u.id,
          u.email,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.avatar,
          u.passwordValid)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(id), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val q = for {
        u <- Users if ! u._deleted && (u.id is java.util.UUID.fromString(id))
      } yield (
          u.id,
          u.email,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.avatar,
          u.passwordValid)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (sId, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(sId), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def removeUser(id: String) = {
      import Database.dynamicSession

      db.withDynSession {
        (Users.forDeletion(id) update(true)) == 1
      }
    }

    def getPurgedUsers = {
      import Database.dynamicSession

      val q = for {
        u <- Users if u._deleted
      } yield (
          u.id,
          u.email,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.avatar,
          u.passwordValid)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(id), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def purgeUsers(users: Set[String]) = db.withTransaction { implicit sesssion =>
      val q = for { u <- Users if u.id inSet (users map java.util.UUID.fromString) } yield u

      users foreach(id => avatars ! utils.Avatars.Purge(id))

      q.delete == users.size
    }

    def getToken(bearerToken: String) = db.withTransaction { implicit session =>

      val q = for {
        (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is bearerToken
      } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)


      q.firstOption map {
        case (accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes) =>

          OAuthTokens map(_.lastAccessTime) update(System.currentTimeMillis) // Touch session

          OAuthToken(accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def getTokenSecret(accessToken: String) = {
      import Database.dynamicSession

      val q = for {
        t <- OAuthTokens if t.accessToken is accessToken
      } yield t.macKey


      db.withDynSession {
        q.firstOption
      }
    }

    def getRefreshToken(refreshToken: String) = {

      val q = for {
        (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.refreshToken is refreshToken
      } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      val result = db.withSession { implicit session =>
        q.firstOption
      }

      result flatMap {
        case (accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, sScopes) =>

          def expired = refreshExpires exists (_ + createdAt < System.currentTimeMillis)

          if(expired) {
            db withTransaction { implicit session =>
              if(!((OAuthTokens where(_.accessToken is accessToken) delete) == 1)) throw new Exception("getRefreshToken: can't delete expired refresh token")
              None
            }
          }
          else Some(OAuthToken(accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, scopes = sScopes))
      }
    }

    def exchangeRefreshToken(refreshToken: String) = db.withTransaction { implicit session =>

      val q = for {
        (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.refreshToken is refreshToken
      } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.uA,
          t.refreshToken,
          t.createdAt,
          t.expiresIn,
          t.refreshExpiresIn,
          t.scopes)

      q.firstOption flatMap {
        case (aAccessToken, clientId, redirectUri, userId, uA, Some(aRefreshToken), issuedTime, expiresIn, refreshExpiresIn, aScopes) if refreshExpiresIn map(t => issuedTime + t * 1000 > System.currentTimeMillis) getOrElse true => //aRefreshToken exists

          def generateToken = utils.SHA3Utils digest s"$clientId:$userId:${System.nanoTime}"
          def generateRefreshToken(accessToken: String) = utils.SHA3Utils digest s"$accessToken:$userId:${System.nanoTime}"
          def generateMacKey = utils.genPasswd(s"$userId:${System.nanoTime}")

          val accessToken = generateToken

          val currentTimestamp = System.currentTimeMillis

          if((OAuthTokens.forInsert +=
            (accessToken, clientId, redirectUri, userId, Some(generateRefreshToken(accessToken)), generateMacKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", aScopes)) != 1)
              throw new Exception("could not refresh Token")

          val q2 = for {
            (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
          } yield (
              t.accessToken,
              t.clientId,
              c.redirectUri,
              t.userId,
              t.refreshToken,
              t.macKey,
              t.uA,
              t.expiresIn,
              t.refreshExpiresIn,
              t.createdAt,
              t.lastAccessTime,
              t.scopes)

          q2.firstOption map {
            case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, dScopes) =>

              if((for {
                t <- OAuthTokens if t.accessToken is aAccessToken
              } yield t).delete != 1) throw new Exception("couldn't create new token")

              OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, scopes = dScopes)
          }

        case _ => None
      }
    }

    def revokeToken(accessToken: String) =
      db.withTransaction { implicit session =>

        val q = for {
          t <- OAuthTokens if t.accessToken is accessToken
        } yield t

        q.delete == 1
      }

    def getUserTokens(userId: String) = {
      import Database.dynamicSession

      val q = for {
        t <- OAuthTokens if (t.userId is java.util.UUID.fromString(userId))
      } yield (
          t.accessToken,
          t.clientId,
          t.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
          OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def getUserSession(params: Map[String, String]) =
      db.withTransaction{ implicit session =>
  //      val userId = params("userId")
        val bearerToken = params("bearerToken")
        val userAgent = params("userAgent")

        val q = for {
          (u, t) <- Users leftJoin OAuthTokens on(_.id is _.userId)
                    if /*(t.userId is java.util.UUID.fromString(userId)) &&
                       */(t.accessToken is bearerToken) &&
                      (t.uA is userAgent)
        } yield (u, (
            t.accessToken,
            t.clientId,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.scopes))

        q.firstOption map {
          case (sUser, (sAccessToken, sClientId, sRefreshToken, sMacKey, sUA, sExpiresIn, sRefreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes)) =>

            import scala.util.control.Exception.allCatch

            allCatch.opt {
              OAuthTokens map(_.lastAccessTime) update(System.currentTimeMillis)
            } // Touch session

            Session(
              sAccessToken,
              sMacKey,
              sClientId,
              sCreatedAt,
              sExpiresIn,
              sRefreshExpiresIn,
              sRefreshToken,
              sLastAccessTime,
              user = sUser copy(password = None),
              userAgent = sUA,
              roles = Set(accessControlService.getUserRoles(sUser.id map(_.toString) get) map(_.role) : _*),
              permissions = {
                val userPermissions = accessControlService.getUserPermissions(sUser.id map(_.toString) get) // TODO: is this dependency safe
                Map(accessControlService.getPermissions map(p => (p.name, userPermissions contains p.name)) : _*)
              },
              scopes = sScopes
            )
        }
      }

    def getClient(id: String, secret: String) = {
      import Database.dynamicSession

      val q = for {
        c <- OAuthClients if (c.id is id) && (c.secret is secret)
      } yield (
          c.id,
          c.secret,
          c.redirectUri)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (cId, cSecret, redirectUri) =>
          OAuthClient(cId, cSecret, redirectUri)
      }
    }

    def authUser(username: String, password: String) = {
      import Database.dynamicSession

      val q = for {
        u <- Users if !u._deleted && (u.email is username)
      } yield (u.id, u.password)

      val result = db.withDynSession {
        q.firstOption
      }

      result collect {
        case (id, sPasswd) if passwords verify(password, sPasswd) => id.toString
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      db.withTransaction { implicit session =>

        val currentTimestamp = System.currentTimeMillis

        if((OAuthTokens.forInsert += (accessToken, clientId, redirectUri, java.util.UUID.fromString(userId), refreshToken, macKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", scopes)) != 1) throw new IllegalArgumentException("can't save token")

        val q = for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
        } yield (
            t.accessToken,
            t.clientId,
            c.redirectUri,
            t.userId,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.scopes)

        q.firstOption map {
          case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
            OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
        }
      }

    def saveUser(email: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) =
      db.withTransaction { implicit session =>
        val id = java.util.UUID.randomUUID

        val currentTimestamp = System.currentTimeMillis

        if((
          Users.forInsert += (id, email, passwords crypt password, firstname, lastname, currentTimestamp, createdBy map java.util.UUID.fromString, Some(currentTimestamp), createdBy map java.util.UUID.fromString, gender, homeAddress, workAddress, contacts, passwordValid)
          ) != 1) throw new IllegalArgumentException("saveUser: can't save user")

        val q = for{
          u <- Users if u.id is id
        } yield (
            u.id,
            u.email,
            u.firstname,
            u.lastname,
            u.createdAt,
            u.createdBy,
            u.lastModifiedAt,
            u.lastModifiedBy,
            u.gender,
            u.homeAddress,
            u.workAddress,
            u.contacts,
            u.avatar,
            u.passwordValid)

        q.firstOption map {
          case (sId, uEmail, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts, sAvatar, sPasswordValid) =>
            User(Some(sId), uEmail, None, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts, sAvatar, passwordValid = sPasswordValid)
        }
      }

    def updateUser(id: String, spec: utils.UserSpec) = {
      val uuid = java.util.UUID.fromString(id)
      val q = schema.Users filter (_.id is uuid)

      db.withSession {
        implicit session => (for {u <- q} yield (u.password, u.contacts)).firstOption
      } match {

        case Some((sPassword, sContacts)) => if (db.withTransaction {
          implicit session =>

            val currentTimestamp = Some(System.currentTimeMillis)

            val _1 = spec.email map {
              email =>
                (q map (_.email) update (email)) == 1
            } getOrElse true

            val _2 = spec.password map {
              password =>
                spec.oldPassword.nonEmpty &&
                  (passwords verify(spec.oldPassword.get, sPassword)) &&
                  ((q map (o => (o.password, o.passwordValid, o.lastModifiedAt, o.lastModifiedBy)) update(passwords crypt password, true, currentTimestamp, Some(uuid))) == 1)
            } getOrElse true

            val _3 = spec.firstname map {
              firstname =>
                (q map (o => (o.firstname, o.lastModifiedAt, o.lastModifiedBy)) update(firstname, currentTimestamp, Some(uuid))) == 1
            } getOrElse true

            val _4 = spec.lastname map {
              lastname =>
                (q map (o => (o.lastname, o.lastModifiedAt, o.lastModifiedBy)) update(lastname, currentTimestamp, Some(uuid))) == 1
            } getOrElse true

            val _5 = spec.gender map {
              gender =>
                (q map (o => (o.gender, o.lastModifiedAt, o.lastModifiedBy)) update(gender, currentTimestamp, Some(uuid))) == 1
            } getOrElse true

            val _6 = spec.homeAddress foreach {
              case homeAddress =>
                (q map (o => (o.homeAddress, o.lastModifiedAt, o.lastModifiedBy)) update(homeAddress, currentTimestamp, Some(uuid))) == 1
            }

            val _7 = spec.workAddress foreach {
              case workAddress =>
                (q map (o => (o.workAddress, o.lastModifiedAt, o.lastModifiedBy)) update(workAddress, currentTimestamp, Some(uuid))) == 1
            }

            val _8 = spec.avatar foreach {
              case Some((avatar, data)) =>
                avatars ! utils.Avatars.Add(id, avatar, data)
                (q map (o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy)) update(Some(avatar), currentTimestamp, Some(uuid))) == 1

              case _ =>
                avatars ! utils.Avatars.Purge(id)
                (q map (o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy)) update(None, currentTimestamp, Some(uuid))) == 1
            }

            val _9 = spec.contacts map (contacts => (q map (o => (o.contacts, o.lastModifiedAt, o.lastModifiedBy)) update(contacts.diff(sContacts), currentTimestamp, Some(uuid))) == 1) getOrElse true

            _1 && _2 && _3 && _4 && _5 && _6 && _7 && _8 && _9
        }) getUser(id)

        else None

        case _ => None
      }
    }

    def getAvatar(id: String) = {
      import scala.concurrent.duration._
      import akka.pattern._
      import scala.util.control.Exception.allCatch
      import scala.concurrent.Await

      implicit val timeout = akka.util.Timeout(60 seconds) // needed for `?` below

      val q = (avatars ? utils.Avatars.Get(id)).mapTo[Option[(domain.AvatarInfo, String)]]

      allCatch.opt {
        Await.result(q, timeout.duration)
      } getOrElse None
    }

    def emailExists(email: String) = {
      import Database.dynamicSession

      val q = {
        val findByEmail = Users.findBy(_.email.toLowerCase)
        findByEmail(email.toLowerCase)
      }

      db.withDynSession {
        Query(q.extract.exists).firstOption
      } getOrElse false
    }
  }
}

trait CachingOAuthServicesComponentImpl extends OAuthServicesComponentImpl with AccessControlServicesComponentImpl{
  self: CachingServicesComponent with OAuthServicesRepoComponent with AccessControlServicesRepoComponent with impl.CacheSystemProvider =>

  import caching._

  import scala.concurrent.duration._
  import akka.pattern._
  import akka.util.Timeout
  import scala.util.control.Exception.allCatch
  import scala.concurrent.Await

  override val oauthService = new CachingOAuthServicesImpl

  class CachingOAuthServicesImpl extends OAuthServicesImpl {

    override def getUsers =
      cachingServices.get[List[domain.User]](ManyParams("users")) { super.getUsers.asInstanceOf[List[domain.User]] } getOrElse Nil

    override def getUser(id: String) =
      cachingServices.get[Option[domain.User]](Params(id)) { super.getUser(id).asInstanceOf[Option[domain.User]] } getOrElse None

    override def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) =
      super.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts, passwordValid) collect{
        case my: domain.User =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def updateUser(id: String, spec: utils.UserSpec) =
      super.updateUser(id, spec) collect {
        case my: domain.User =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def removeUser(id: String): Boolean =
      if(super.removeUser(id)){
        cachingServices.purge(Params(id))
        cachingServices.purge(ManyParams("users"))
        true
      } else false

    override def purgeUsers(users: Set[String]) {
      super.purgeUsers(users)
      users foreach(o => cachingServices.purge(Params(o)))
      cachingServices.purge(ManyParams("users"))
    }
  }

  override val accessControlService = new CachingAccessControlServicesImpl

  class CachingAccessControlServicesImpl extends AccessControlServicesImpl {

    override def getRoles =
      cachingServices.get[List[domain.Role]](ManyParams("roles")) { super.getRoles.asInstanceOf[List[domain.Role]] } getOrElse Nil

    override def getRole(roleName: String) =
      cachingServices.get[Option[domain.Role]](Params(roleName)) { super.getRole(roleName).asInstanceOf[Option[domain.Role]] } getOrElse None

    override def saveRole(name: String, parent: Option[String], createdBy: Option[String]) =
      super.saveRole(name, parent, createdBy) collect{
        case my: domain.Role =>
          cachingServices.purge(Params(my.name))
          cachingServices.purge(ManyParams("roles"))
          my
      }

    override def updateRole(name: String, newName: String, parent: Option[String]) =
      if(super.updateRole(name, newName, parent)) {
        cachingServices.purge(Params(newName))
        cachingServices.purge(ManyParams("roles"))
        true
      } else false

    override def purgeRoles(roles: Set[String]) {
      super.purgeRoles(roles)
      roles foreach(o => cachingServices.purge(Params(o)))
      cachingServices.purge(ManyParams("roles"))
    }
  }
}

trait CachingServicesComponentImpl extends CachingServicesComponent {
  self: impl.CacheSystemProvider =>

  import scala.concurrent.duration._
  import akka.pattern._
  import akka.util.Timeout
  import scala.util.control.Exception.allCatch
  import scala.concurrent.Await

  protected val cachingServices = new CachingServicesImpl

  protected lazy val cacheActor = cacheSystem.createCacheActor("OAdmin", new impl.OAdminCacheActor(_))

  class CachingServicesImpl extends CachingServices {

    def get[T : scala.reflect.ClassTag](params: impl.CacheActor.Params)(default: => T): Option[T] = {
      implicit val tm = Timeout(10 seconds)

      val q = (cacheActor ? impl.CacheActor.FindValue(params, () => default)).mapTo[Option[T]]

      allCatch.opt {
        Await.result(q, tm.duration)
      } getOrElse None
    }

    def purge(params: impl.CacheActor.Params) {
      cacheActor ! impl.CacheActor.PurgeValue(params)
    }
  }
}