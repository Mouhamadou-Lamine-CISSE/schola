package ma.epsilon.schola
package domain

sealed trait UpdateSpec[T] {
  def set: Option[Option[T]]

  @inline final def foreach(f: Option[T] => Boolean) = set map f getOrElse true

  @inline final def isEmpty = set eq None
}

case class UpdateSpecImpl[T](set: Option[Option[T]] = None) extends UpdateSpec[T]

case class ContactInfoSpec(
  email: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  fax: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  phoneNumber: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

case class AddressInfoSpec(
  city: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  country: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  postalCode: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  streetAddress: UpdateSpecImpl[String] = UpdateSpecImpl[String]())


trait UserSpec {  

  case class MobileNumbersSpec(
    mobile1: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    mobile2: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactsSpec(
    mobiles: UpdateSpecImpl[MobileNumbersSpec] = UpdateSpecImpl[MobileNumbersSpec](),
    home: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec](),
    work: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]())

  def contacts: UpdateSpec[ContactsSpec]

  def homeAddress: UpdateSpec[AddressInfoSpec]

  def workAddress: UpdateSpec[AddressInfoSpec]

  def cin: Option[String]

  def stars: Option[Int]

  def primaryEmail: Option[String]

  def password: Option[String] // Though this is an Option, its required!

  def oldPassword: Option[String]

  def givenName: Option[String]

  def familyName: Option[String]

  def gender: Option[Gender]

  def avatar: UpdateSpec[String]

  def accessRights: Option[Set[String]]

  def suspended: Option[Boolean]

  def updatedBy: Option[String]
}

class DefaultUserSpec extends UserSpec {

  lazy val contacts = UpdateSpecImpl[ContactsSpec]()

  lazy val homeAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val workAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val cin: Option[String] = None

  lazy val primaryEmail: Option[String] = None

  lazy val password: Option[String] = None

  lazy val oldPassword: Option[String] = None

  lazy val givenName: Option[String] = None

  lazy val familyName: Option[String] = None

  lazy val stars: Option[Int] = None

  lazy val gender: Option[Gender.Value] = None

  lazy val avatar = UpdateSpecImpl[String]()

  lazy val accessRights: Option[Set[String]] = None

  lazy val suspended: Option[Boolean] = None

  lazy val updatedBy: Option[String] = None
}