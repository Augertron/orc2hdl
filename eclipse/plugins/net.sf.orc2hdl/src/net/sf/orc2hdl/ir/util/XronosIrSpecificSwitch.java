/**
 */
package net.sf.orc2hdl.ir.util;

import net.sf.orc2hdl.ir.*;

import net.sf.orcc.ir.InstSpecific;
import net.sf.orcc.ir.Instruction;

import net.sf.orcc.util.Attributable;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see net.sf.orc2hdl.ir.XronosIrSpecificPackage
 * @generated
 */
public class XronosIrSpecificSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static XronosIrSpecificPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public XronosIrSpecificSwitch() {
		if (modelPackage == null) {
			modelPackage = XronosIrSpecificPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @parameter ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case XronosIrSpecificPackage.INST_PORT_STATUS: {
				InstPortStatus instPortStatus = (InstPortStatus)theEObject;
				T result = caseInstPortStatus(instPortStatus);
				if (result == null) result = caseInstSpecific(instPortStatus);
				if (result == null) result = caseInstruction(instPortStatus);
				if (result == null) result = caseAttributable(instPortStatus);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrSpecificPackage.INST_PORT_READ: {
				InstPortRead instPortRead = (InstPortRead)theEObject;
				T result = caseInstPortRead(instPortRead);
				if (result == null) result = caseInstSpecific(instPortRead);
				if (result == null) result = caseInstruction(instPortRead);
				if (result == null) result = caseAttributable(instPortRead);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrSpecificPackage.INST_PORT_WRITE: {
				InstPortWrite instPortWrite = (InstPortWrite)theEObject;
				T result = caseInstPortWrite(instPortWrite);
				if (result == null) result = caseInstSpecific(instPortWrite);
				if (result == null) result = caseInstruction(instPortWrite);
				if (result == null) result = caseAttributable(instPortWrite);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case XronosIrSpecificPackage.INST_PORT_PEEK: {
				InstPortPeek instPortPeek = (InstPortPeek)theEObject;
				T result = caseInstPortPeek(instPortPeek);
				if (result == null) result = caseInstSpecific(instPortPeek);
				if (result == null) result = caseInstruction(instPortPeek);
				if (result == null) result = caseAttributable(instPortPeek);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Status</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Status</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortStatus(InstPortStatus object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Read</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Read</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortRead(InstPortRead object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Write</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Write</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortWrite(InstPortWrite object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Port Peek</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Port Peek</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstPortPeek(InstPortPeek object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Attributable</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Attributable</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAttributable(Attributable object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Instruction</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Instruction</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstruction(Instruction object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Inst Specific</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Inst Specific</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInstSpecific(InstSpecific object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //XronosIrSpecificSwitch
