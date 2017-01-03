package org.grails.datastore.mapping.model

import org.grails.datastore.mapping.model.config.JpaMappingConfigurationStrategy
import org.grails.datastore.mapping.model.types.Association
import spock.lang.Specification
import javax.persistence.*

/**
 * Created by jameskleeh on 12/21/16.
 */
class JpaMappingSyntaxTests extends Specification {
    
    void "test class is entity"() {
        given:
        def strategy = new JpaMappingConfigurationStrategy(new TestMappedPropertyFactory())

        expect:
        strategy.isPersistentEntity(JpaTestEntity)
        strategy.isPersistentEntity(JavaEntity)
        !strategy.isPersistentEntity(JpaMappingSyntaxTests)
    }
    
    void "test get identity"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaTestEntity)
        def strategy = context.mappingSyntaxStrategy
        def id = strategy.getIdentity(JpaTestEntity, context)

        expect:
        id != null
        id.type == Long
        id.name == 'customId'
    }

    void "test get composite identity"() {
        given:
        def context = new TestMappingContext()
        PersistentEntity entity = context.addPersistentEntity(JpaCompositeIdEntity)

        expect:
        entity.identity == null
        entity.compositeIdentity != null
        entity.compositeIdentity.size() == 2
        entity.persistentProperties.size() == 1
    }

    
    void "test get simple persistent properties"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaTestEntity)
        context.addPersistentEntity(JpaSecondEntity)
        def strategy = context.mappingSyntaxStrategy
        
        when:
        def props = strategy.getPersistentProperties(JpaTestEntity,context)
        
        then:
        props.size() == 4
    }
    
    void "test unidirectional one to one"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaTestEntity)

        expect:
        assert 2 == context.persistentEntities.size()

        when:
        def testEntity = context.getPersistentEntity(JpaTestEntity.name)
        def association = testEntity.getPropertyByName("second")
        org.grails.datastore.mapping.model.types.OneToOne toOne = association

        then:
        association != null
        (association instanceof org.grails.datastore.mapping.model.types.OneToOne)
        toOne.foreignKeyInChild
        toOne.associatedEntity != null
        toOne.associatedEntity == context.getPersistentEntity(JpaSecondEntity.name)
        toOne.referencedPropertyName == null
        !toOne.bidirectional
        toOne.owningSide
    }


    void "test uni-directional one to many"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaPublisher)

        expect:
        3 == context.persistentEntities.size()

        when:
        def publisher = context.getPersistentEntity(JpaPublisher.name)
        Association oneToMany = publisher.getPropertyByName("authors")
        
        then:
        publisher != null
        oneToMany != null
        !oneToMany.bidirectional
        !oneToMany.owningSide
        (oneToMany instanceof org.grails.datastore.mapping.model.types.OneToMany)
    }

    void "test many to one association"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaBook)

        expect:
        2 == context.persistentEntities.size()

        when:
        def book = context.getPersistentEntity(JpaBook.name)
        Association authorAssociation = book.getPropertyByName("author")
        Association inverse = authorAssociation.inverseSide


        then:
        book != null
        authorAssociation != null
        (authorAssociation instanceof org.grails.datastore.mapping.model.types.ManyToOne)
        authorAssociation.bidirectional
        !authorAssociation.owningSide

        inverse != null

        "books" == inverse.name
        JpaAuthor == inverse.owner.javaClass
        inverse.inverseSide != null
        inverse.bidirectional
        (inverse instanceof org.grails.datastore.mapping.model.types.OneToMany)
        inverse.owningSide
    }


    void "test indexed property"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaEntityWithIndexedProperty)

        expect:
        1 == context.persistentEntities.size()
        context.getPersistentEntity(JpaEntityWithIndexedProperty.name) != null
        3 == context.mappingSyntaxStrategy.getPersistentProperties(JpaEntityWithIndexedProperty, context).size()
    }
    
    void "test force unidirectional"() {
        given:
        def context = new TestMappingContext()
        context.addPersistentEntity(JpaUser)
        
        expect:
        1 == context.persistentEntities.size()

        when:
        def user = context.getPersistentEntity(JpaUser.name)
        Association foesAssociation = user.getPropertyByName("foes")
        Association friendsAssociation = user.getPropertyByName("friends")
        Association bestBuddyAssociation = user.getPropertyByName("bestBuddy")
        
        then:
        (foesAssociation instanceof org.grails.datastore.mapping.model.types.OneToMany)
        !foesAssociation.isBidirectional()
        
        (friendsAssociation instanceof org.grails.datastore.mapping.model.types.OneToMany)
        !friendsAssociation.isBidirectional()
        
        (bestBuddyAssociation instanceof org.grails.datastore.mapping.model.types.OneToOne)
        !bestBuddyAssociation.isBidirectional()

    }

    @javax.persistence.Entity
    class JavaEntity {}
}

@javax.persistence.Entity
class JpaBook {
    @Id
    Long id
    
    String title
    
    @ManyToOne
    JpaAuthor author
}

@javax.persistence.Entity
class JpaAuthor {
    @Id
    Long id
    
    String name
    
    @OneToMany(mappedBy = "author")
    Set<JpaBook> books
    
    def shouldBeIgnored
}

@javax.persistence.Entity
class JpaPublisher {
    @Id
    Long id
    
    @OneToMany
    Set<JpaAuthor> authors
}

@javax.persistence.Entity
class JpaTestEntity {
    @Id
    Long customId
    Long version
    String name

    @Transient
    String bar

    @OneToOne
    JpaSecondEntity second
}

@javax.persistence.Entity
class JpaSecondEntity {
    @Id
    Long id

    String name

    @Transient
    String bar
}

@javax.persistence.Entity
class JpaEntityWithIndexedProperty {
    @Id
    Long id
    Long version
    String name
    String bar

    String getSectionContent(int section) {}
    void setSectionContent(int section, String content) {}
}

@javax.persistence.Entity
class JpaUser {
    @Id
    Long id
    Long version
    String name
    @OneToOne
    JpaUser bestBuddy
    @OneToMany
    Set<JpaUser> foes
    @OneToMany
    Set<JpaUser> friends
}


@Entity
class JpaCompositeIdEntity {
    @Id
    Long first
    @Id
    Long second
    String name
}