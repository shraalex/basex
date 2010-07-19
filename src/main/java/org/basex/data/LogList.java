package org.basex.data;

/**
 * A pseudo-linear logging List to realize the ID->Pre Mapping. It saves 
 * the inserts and deletes into the Database and calculates the mapping.
 * Works together with the Node-class.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Philipp Ziemer
 */
public class LogList {
  /** First Node in the list. */
  Node first;
  
  /**
   * Creates an empty LogList.
   */
  public LogList() {
    first = null;
  }
  
  /**
   * Logs a the insert of a new item into the document.
   * @param id value of the entry.
   * @param pre value of the entry.
   */
  public void insert(final int id, final int pre) {   
    // list is empty
    if(first == null) {     
      // calculation of the addends
      int mainAddend = 1;
      int subAddend = pre - id - 1;
      
      // insert the mainNode and then the subNode
      first = new Node(pre, mainAddend);
      first.addSub(id, subAddend);
    // list has entries
    } else {
      // search right position for insert through linear search
      Node pointer = first;
      Node b4pointer = null; // node before pointer
      int addend = 0; // needed to calculate correct value for the sub-node
      while(pointer != null && pointer.id < pre) {
        addend += pointer.addend;
        b4pointer = pointer;
        pointer = pointer.next;
      }
      
      // handle node with pre exists
      if(pointer != null && pointer.id == pre) {
        ++pointer.addend;
        // addend of subnode is pre - id - addend - addend of pointer 
        // (which is not counted yet)
        pointer.addSub(id, pre - id - addend - pointer.addend);
      // new node needs to be created
      } else {
        // built up new node and subnode
        Node newNode = new Node(pre, 1);
        // addend of subnode is pre - id - addend - addend of pointer 
        // (which is not counted yet)
        newNode.addSub(id, pre - id - addend - 1);
        
        // insert into list
        // check if item will be first item
        if(b4pointer == null) // yes
          first = newNode;          
        else // no
          b4pointer.next = newNode;
        newNode.next = pointer;       
      }     
    }
  }
  
  /**
   * Logs a the delete of a new item into the document.
   * @param pre value of the entry.
   */
  public void delete(final int pre) {
    // list is empty
    if(first == null)       
      // insert the mainNode
      first = new Node(pre, -1);
    // list has entries
    else {
      // search right position for insert through linear search
      Node pointer = first;
      Node b4pointer = null; // node before pointer
      int addend = 0; // needed to calculate correct value for the sub-node
      while(pointer != null && pointer.id < pre) {
        addend += pointer.addend;
        b4pointer = pointer;
        pointer = pointer.next;
      }
      
      // handle node with pre exists
      if(pointer != null && pointer.id == pre)
        --pointer.addend;
      // new node needs to be created
      else {
        // built up new node and subnode
        Node newNode = new Node(pre, -1);
        
        // insert into list
        // check if item will be first item
        if(b4pointer == null) // yes
          first = newNode;          
        else // no
          b4pointer.next = newNode;
        newNode.next = pointer;       
      }
    }
  }
  
  /**
   * Calculates the Pre-value for a given ID.
   * @param id value of the entry.
   * @return Pre-value.
   */
  public int pre(final int id) {
    int addend = 0;
    Node pointer = first;
    
    while(pointer != null) {
      // check if id is still affected
      if(pointer.id <= id) {
        addend += pointer.addend;
        
        // check for subitem
        if(pointer.hasSub()) {
          int subaddend = pointer.getSubAddend(id);
          if(subaddend != Integer.MIN_VALUE) {
            addend += subaddend;
            pointer = null; // subitem found => end
          } 
        }
        
        if(pointer != null)
          pointer = pointer.next;
      } else pointer = null;
    }
    
    return id + addend;
  }
}