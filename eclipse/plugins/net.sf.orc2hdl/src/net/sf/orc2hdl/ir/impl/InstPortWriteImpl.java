/**
 */
package net.sf.orc2hdl.ir.impl;

import net.sf.orc2hdl.ir.InstPortWrite;
import net.sf.orc2hdl.ir.XronosIrSpecificPackage;
import net.sf.orcc.df.Port;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.impl.InstSpecificImpl;
import net.sf.orcc.ir.util.ExpressionPrinter;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Inst Port Write</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.sf.orc2hdl.ir.impl.InstPortWriteImpl#getPort <em>Port</em>}</li>
 * <li>{@link net.sf.orc2hdl.ir.impl.InstPortWriteImpl#getValue <em>Value</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class InstPortWriteImpl extends InstSpecificImpl implements
		InstPortWrite {
	/**
	 * The cached value of the '{@link #getPort() <em>Port</em>}' reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getPort()
	 * @generated
	 * @ordered
	 */
	protected Vertex port;

	/**
	 * The cached value of the '{@link #getValue() <em>Value</em>}' containment
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getValue()
	 * @generated
	 * @ordered
	 */
	protected Expression value;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected InstPortWriteImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return XronosIrSpecificPackage.Literals.INST_PORT_WRITE;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Vertex getPort() {
		return port;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setPort(Vertex newPort) {
		Vertex oldPort = port;
		port = newPort;
		if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrSpecificPackage.INST_PORT_WRITE__PORT, oldPort,
					port));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Expression getValue() {
		return value;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public NotificationChain basicSetValue(Expression newValue,
			NotificationChain msgs) {
		Expression oldValue = value;
		value = newValue;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this,
					Notification.SET,
					XronosIrSpecificPackage.INST_PORT_WRITE__VALUE, oldValue,
					newValue);
			if (msgs == null) {
				msgs = notification;
			} else {
				msgs.add(notification);
			}
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setValue(Expression newValue) {
		if (newValue != value) {
			NotificationChain msgs = null;
			if (value != null) {
				msgs = ((InternalEObject) value)
						.eInverseRemove(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrSpecificPackage.INST_PORT_WRITE__VALUE,
								null, msgs);
			}
			if (newValue != null) {
				msgs = ((InternalEObject) newValue)
						.eInverseAdd(
								this,
								EOPPOSITE_FEATURE_BASE
										- XronosIrSpecificPackage.INST_PORT_WRITE__VALUE,
								null, msgs);
			}
			msgs = basicSetValue(newValue, msgs);
			if (msgs != null) {
				msgs.dispatch();
			}
		} else if (eNotificationRequired()) {
			eNotify(new ENotificationImpl(this, Notification.SET,
					XronosIrSpecificPackage.INST_PORT_WRITE__VALUE, newValue,
					newValue));
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd,
			int featureID, NotificationChain msgs) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_WRITE__VALUE:
			return basicSetValue(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_WRITE__PORT:
			return getPort();
		case XronosIrSpecificPackage.INST_PORT_WRITE__VALUE:
			return getValue();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_WRITE__PORT:
			setPort((Vertex) newValue);
			return;
		case XronosIrSpecificPackage.INST_PORT_WRITE__VALUE:
			setValue((Expression) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_WRITE__PORT:
			setPort((Vertex) null);
			return;
		case XronosIrSpecificPackage.INST_PORT_WRITE__VALUE:
			setValue((Expression) null);
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case XronosIrSpecificPackage.INST_PORT_WRITE__PORT:
			return port != null;
		case XronosIrSpecificPackage.INST_PORT_WRITE__VALUE:
			return value != null;
		}
		return super.eIsSet(featureID);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString());
		builder.append("PortWrite(").append(((Port) port).getName());
		return builder.append(", ")
				.append(new ExpressionPrinter().doSwitch(getValue()))
				.append(")").toString();
	}
} // InstPortWriteImpl
