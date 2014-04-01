package ma.epsilon.schola

import domain._

object Types {

  type UserLike = {
    val id: Option[java.util.UUID]
    val primaryEmail: String
    val password: Option[String]
    val givenName: String
    val familyName: String
    val createdAt: Long
    val createdBy: Option[java.util.UUID]
    val lastLoginTime: Option[Long]
    val lastModifiedAt: Option[Long]
    val lastModifiedBy: Option[java.util.UUID]
    val gender: domain.Gender
    val homeAddress: Option[domain.AddressInfo]
    val workAddress: Option[domain.AddressInfo]
    val contacts: Option[domain.Contacts]
    val _deleted: Boolean
    val suspended: Boolean
    val changePasswordAtNextLogin: Boolean
    val labels: List[String]
    val accessRights: List[AccessRight]
  }

  type ProfileLike = {
    val id: java.util.UUID
    val primaryEmail: String
    val givenName: String
    val familyName: String
    val createdAt: Long
    val createdBy: Option[java.util.UUID]
    val lastModifiedAt: Option[Long]
    val lastModifiedBy: Option[java.util.UUID]
    val gender: domain.Gender
    val homeAddress: Option[domain.AddressInfo]
    val workAddress: Option[domain.AddressInfo]
    val contacts: Option[domain.Contacts]
  }

  type UserLabelLike = {
    val userId: java.util.UUID
    val label: String
  }

  type StatsLike = {
    val count: Int
  }

  //

  type TokenLike = {
    val accessToken: String
    val clientId: String
    val redirectUri: String
    val userId: java.util.UUID
    val refreshToken: Option[String]
    val macKey: String
    val uA: String
    val expiresIn: Option[Long]
    val refreshExpiresIn: Option[Long]
    val createdAt: Long
    val lastAccessTime: Long
    val tokenType: String
    val accessRights: Set[AccessRight]
  }

  type ClientLike = {
    val id: String
    val secret: String
    val redirectUri: String
  }

  type SessionLike = {
    val key: String
    val secret: String
    val clientId: String
    val issuedTime: Long
    val expiresIn: Option[Long]
    val refreshExpiresIn: Option[Long]
    val refresh: Option[String]
    val lastAccessTime: Long
    val superUser: Boolean
    val suspended: Boolean
    val changePasswordAtNextLogin: Boolean
    val user: ProfileLike
    val userAgent: String
    val accessRights: Set[AccessRight]
  }

  // 

  type LabelLike = {
    val name: String
    val color: String
  }
}
